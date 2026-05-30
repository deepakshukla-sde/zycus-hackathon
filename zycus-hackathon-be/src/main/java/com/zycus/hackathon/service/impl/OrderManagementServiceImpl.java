package com.zycus.hackathon.service.impl;

import com.zycus.hackathon.dto.*;
import com.zycus.hackathon.entity.Agent;
import com.zycus.hackathon.entity.AgentStatus;
import com.zycus.hackathon.entity.Order;
import com.zycus.hackathon.entity.OrderStatus;
import com.zycus.hackathon.repository.AgentRepository;
import com.zycus.hackathon.repository.OrderRepository;
import com.zycus.hackathon.service.LLMGateway;
import com.zycus.hackathon.service.OrderManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderManagementServiceImpl implements OrderManagementService {

    private final AgentRepository agentRepository;
    private final OrderRepository orderRepository;
    private final LLMGateway llmGateway;

    @Override
    public List<AgentResponse> getAllAgents() {
        return agentRepository.findAll().stream()
                .map(this::toAgentResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderResponse> getAllOrders() {
        List<Order> orders = orderRepository.findAll();

        List<String> agentIds = orders.stream()
                .filter(o -> o.getAssignedAgentId() != null)
                .map(Order::getAssignedAgentId)
                .distinct()
                .collect(Collectors.toList());

        Map<String, String> agentNames = agentRepository.findAllById(agentIds).stream()
                .collect(Collectors.toMap(Agent::getId, Agent::getName));

        return orders.stream()
                .map(o -> toOrderResponse(o, agentNames.getOrDefault(o.getAssignedAgentId(), null)))
                .collect(Collectors.toList());
    }

    @Override
    public AssignOrderResponse assignOrders() {
        // 1. Fetch all pending orders
        List<Order> pendingOrders = orderRepository.findByStatus(OrderStatus.PENDING);
        if (pendingOrders.isEmpty()) {
            return AssignOrderResponse.builder()
                    .success(false)
                    .totalAssigned(0)
                    .totalFailed(0)
                    .assignments(List.of())
                    .message("No pending orders to assign.")
                    .build();
        }

        // 2. Fetch all available agents
        List<Agent> availableAgents = agentRepository.findByStatus(AgentStatus.AVAILABLE);
        if (availableAgents.isEmpty()) {
            return AssignOrderResponse.builder()
                    .success(false)
                    .totalAssigned(0)
                    .totalFailed(pendingOrders.size())
                    .assignments(List.of())
                    .message("No available agents at this time. " + pendingOrders.size() + " order(s) could not be assigned.")
                    .build();
        }

        // 3. Build context strings for LLM
        String ordersContext = pendingOrders.stream()
                .map(o -> o.getId() + " | " + o.getDescription())
                .collect(Collectors.joining("\n"));

        String agentsContext = availableAgents.stream()
                .map(a -> a.getId() + " | " + a.getName()
                        + " | Active Orders: " + a.getActiveOrderCount()
                        + " | Rating: " + (a.getRating() != null ? a.getRating() : "N/A"))
                .collect(Collectors.joining("\n"));

        log.info("Sending {} pending orders and {} available agents to LLM", pendingOrders.size(), availableAgents.size());

        // 4. Call LLM for bulk assignment
        AssignOrderResponse llmResponse = llmGateway.assignOrders(ordersContext, agentsContext);

        // 5. Persist valid assignments returned by LLM
        if (llmResponse.getAssignments() != null) {
            Map<String, Order> orderMap = pendingOrders.stream()
                    .collect(Collectors.toMap(Order::getId, o -> o));
            Map<String, Agent> agentMap = availableAgents.stream()
                    .collect(Collectors.toMap(Agent::getId, a -> a));

            for (AssignOrderResponse.AssignmentDetail detail : llmResponse.getAssignments()) {
                if (!detail.isSuccess()) continue;

                Order order = orderMap.get(detail.getOrderId());
                Agent agent = agentMap.get(detail.getAssignedAgentId());

                if (order == null || agent == null) {
                    log.warn("Skipping invalid assignment - order: {}, agent: {}", detail.getOrderId(), detail.getAssignedAgentId());
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

                // Enrich detail with agent name for response
                detail.setAgentName(agent.getName());

                log.info("Persisted assignment: order {} -> agent {}", order.getId(), agent.getId());
            }
        }

        return llmResponse;
    }

    @Override
    public OrderResponse createOrder(CreateOrderRequest request) {

        Order order = Order.builder()
                .id("ORD-" + System.currentTimeMillis())
                .description(request.getDescription())
                .assignedAgentId(null)
                .status(OrderStatus.PENDING)
                .build();

        Order savedOrder = orderRepository.save(order);

        return OrderResponse.builder()
                .id(savedOrder.getId())
                .description(savedOrder.getDescription())
                .assignedAgentId(savedOrder.getAssignedAgentId())
                .status(OrderStatus.valueOf(savedOrder.getStatus().name()))
                .build();
    }

    @Override
    public AgentResponse updateAgentStatus(
            String agentId,
            UpdateAgentStatusRequest request) {

        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Agent not found: " + agentId));

        agent.setStatus(request.getStatus());

        Agent updatedAgent = agentRepository.save(agent);

        return AgentResponse.builder()
                .id(updatedAgent.getId())
                .name(updatedAgent.getName())
                .activeOrderCount(updatedAgent.getActiveOrderCount())
                .rating(updatedAgent.getRating())
                .status(AgentStatus.valueOf(updatedAgent.getStatus().name()))
                .build();
    }

    private AgentResponse toAgentResponse(Agent agent) {
        return AgentResponse.builder()
                .id(agent.getId())
                .name(agent.getName())
                .activeOrderCount(agent.getActiveOrderCount())
                .status(agent.getStatus())
                .rating(agent.getRating())
                .build();
    }

    private OrderResponse toOrderResponse(Order order, String agentName) {
        return OrderResponse.builder()
                .id(order.getId())
                .description(order.getDescription())
                .assignedAgentId(order.getAssignedAgentId())
                .assignedAgentName(agentName)
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .build();
    }
}