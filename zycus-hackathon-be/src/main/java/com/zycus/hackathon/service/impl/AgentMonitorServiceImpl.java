package com.zycus.hackathon.service.impl;

import com.zycus.hackathon.entity.Agent;
import com.zycus.hackathon.entity.AgentStatus;
import com.zycus.hackathon.entity.Order;
import com.zycus.hackathon.entity.OrderStatus;
import com.zycus.hackathon.repository.AgentRepository;
import com.zycus.hackathon.repository.OrderRepository;
import com.zycus.hackathon.service.AgentMonitorService;
import com.zycus.hackathon.service.LLMGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentMonitorServiceImpl implements AgentMonitorService {

    private final AgentRepository agentRepository;
    private final OrderRepository orderRepository;
    private final LLMGateway llmGateway;

    @Override
    @Scheduled(fixedDelayString = "${app.polling.interval:3000}")
    public void checkAndReassignOfflineAgents() {
        log.debug("Polling: checking for offline agents with assigned orders...");

        // 1. Find all OFFLINE agents
        List<Agent> offlineAgents = agentRepository.findByStatus(AgentStatus.OFFLINE);
        if (offlineAgents.isEmpty()) {
            log.debug("Polling: no offline agents found.");
            return;
        }

        log.info("Polling: found {} offline agent(s). Checking for assigned orders...", offlineAgents.size());

        // 2. Find all orders assigned to offline agents
        List<String> offlineAgentIds = offlineAgents.stream()
                .map(Agent::getId)
                .collect(Collectors.toList());

        List<Order> affectedOrders = orderRepository.findByStatusAndAssignedAgentIdIn(
                OrderStatus.ASSIGNED, offlineAgentIds);

        if (affectedOrders.isEmpty()) {
            log.info("Polling: offline agents have no assigned orders. Nothing to reassign.");
            return;
        }

        log.info("Polling: {} order(s) need reassignment due to offline agents.", affectedOrders.size());

        // 3. Find available agents sorted by rating descending
        List<Agent> availableAgents = agentRepository.findByStatus(AgentStatus.AVAILABLE)
                .stream()
                .sorted((a, b) -> {
                    double ratingA = a.getRating() != null ? a.getRating() : 0.0;
                    double ratingB = b.getRating() != null ? b.getRating() : 0.0;
                    return Double.compare(ratingB, ratingA);
                })
                .collect(Collectors.toList());

        if (availableAgents.isEmpty()) {
            log.warn("Polling: no available agents to reassign {} order(s). Will retry next poll.", affectedOrders.size());
            // Reset orders to PENDING so they can be picked up later
            affectedOrders.forEach(order -> {
                order.setAssignedAgentId(null);
                order.setStatus(OrderStatus.PENDING);
                orderRepository.save(order);
            });
            return;
        }

        // 4. Build context and call LLM for reassignment
        String ordersContext = affectedOrders.stream()
                .map(o -> o.getId() + " | " + o.getDescription())
                .collect(Collectors.joining("\n"));

        String agentsContext = availableAgents.stream()
                .map(a -> a.getId() + " | " + a.getName()
                        + " | Active Orders: " + a.getActiveOrderCount()
                        + " | Rating: " + (a.getRating() != null ? a.getRating() : "N/A"))
                .collect(Collectors.joining("\n"));

        log.info("Polling: calling LLM to reassign {} order(s) to {} available agent(s).",
                affectedOrders.size(), availableAgents.size());

        var llmResponse = llmGateway.assignOrders(ordersContext, agentsContext);

        if (llmResponse == null || llmResponse.getAssignments() == null) {
            log.warn("Polling: LLM returned no assignments. Resetting orders to PENDING.");
            affectedOrders.forEach(order -> {
                order.setAssignedAgentId(null);
                order.setStatus(OrderStatus.PENDING);
                orderRepository.save(order);
            });
            return;
        }

        // 5. Persist reassignments
        Map<String, Order> orderMap = affectedOrders.stream()
                .collect(Collectors.toMap(Order::getId, o -> o));
        Map<String, Agent> agentMap = availableAgents.stream()
                .collect(Collectors.toMap(Agent::getId, a -> a));

        // 6. Decrement active order count for offline agents
        offlineAgents.forEach(agent -> {
            long count = affectedOrders.stream()
                    .filter(o -> agent.getId().equals(o.getAssignedAgentId()))
                    .count();
            int updated = Math.max(0, agent.getActiveOrderCount() - (int) count);
            agent.setActiveOrderCount(updated);
            agentRepository.save(agent);
        });

        for (var detail : llmResponse.getAssignments()) {
            if (!detail.isSuccess()) {
                // LLM could not assign — reset to PENDING
                Order order = orderMap.get(detail.getOrderId());
                if (order != null) {
                    order.setAssignedAgentId(null);
                    order.setStatus(OrderStatus.PENDING);
                    orderRepository.save(order);
                    log.warn("Polling: could not reassign order {}. Reset to PENDING.", detail.getOrderId());
                }
                continue;
            }

            Order order = orderMap.get(detail.getOrderId());
            Agent agent = agentMap.get(detail.getAssignedAgentId());

            if (order == null || agent == null) {
                log.warn("Polling: invalid reassignment - order: {}, agent: {}",
                        detail.getOrderId(), detail.getAssignedAgentId());
                continue;
            }

            // Update order
            order.setAssignedAgentId(agent.getId());
            order.setStatus(OrderStatus.ASSIGNED);
            orderRepository.save(order);

            // Update agent
            agent.setActiveOrderCount(agent.getActiveOrderCount() + 1);
            agent.setStatus(AgentStatus.BUSY);
            agentMap.put(agent.getId(), agent);
            agentRepository.save(agent);

            log.info("Polling: reassigned order {} from offline agent to {}",
                    order.getId(), agent.getId());
        }

        log.info("Polling: reassignment complete. {}/{} order(s) successfully reassigned.",
                llmResponse.getTotalAssigned(), affectedOrders.size());
    }
}