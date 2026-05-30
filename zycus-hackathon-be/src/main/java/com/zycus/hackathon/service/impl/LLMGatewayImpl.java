package com.zycus.hackathon.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zycus.hackathon.dto.AssignOrderResponse;
import com.zycus.hackathon.entity.Agent;
import com.zycus.hackathon.entity.AgentStatus;
import com.zycus.hackathon.entity.Order;
import com.zycus.hackathon.entity.OrderStatus;
import com.zycus.hackathon.repository.AgentRepository;
import com.zycus.hackathon.repository.OrderRepository;
import com.zycus.hackathon.service.LLMGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class LLMGatewayImpl implements LLMGateway {

    @Value("${llm.provider}")
    private String provider;

    @Value("${llm.api-key:}")
    private String apiKey;

    @Value("${llm.model}")
    private String model;

    @Value("${llm.base-url}")
    private String baseUrl;

    private final RestClient http = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final AgentRepository agentRepository;
    private final OrderRepository orderRepository;

    public LLMGatewayImpl(AgentRepository agentRepository, OrderRepository orderRepository) {
        this.agentRepository = agentRepository;
        this.orderRepository = orderRepository;
    }

    @Override
    public AssignOrderResponse assignOrders(String ordersContext, String agentsContext) {
        log.info("LLM provider selected: {}", provider);
        return switch (provider.toLowerCase()) {
            case "gpt" -> assignWithGpt(ordersContext, agentsContext);
            default -> {
                log.warn("Unknown provider '{}'. No assignment made.", provider);
                yield AssignOrderResponse.builder()
                        .success(false)
                        .totalAssigned(0)
                        .totalFailed(0)
                        .assignments(List.of())
                        .message("Unknown LLM provider configured.")
                        .build();
            }
        };
    }

    private AssignOrderResponse assignWithGpt(String ordersContext, String agentsContext) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("OpenAI API key not configured. No assignment made.");
            return AssignOrderResponse.builder()
                    .success(false)
                    .totalAssigned(0)
                    .totalFailed(0)
                    .assignments(List.of())
                    .message("LLM API key not configured.")
                    .build();
        }
        try {
            String prompt = buildPrompt(ordersContext, agentsContext);
            String rawResponse = callOpenAI(prompt);
            log.info("LLM raw assignment response: {}", rawResponse);
            return parseAssignments(rawResponse);
        } catch (Exception e) {
            log.warn(
                    "GPT bulk assignment failed. Using fallback assignment. Error: {}",
                    e.getMessage());

            return assignUsingFallbackRules(
                    ordersContext,
                    agentsContext
            );
        }
    }

    private String buildPrompt(String ordersContext, String agentsContext) {
        return """
                You are a smart procurement order assignment system.
                
                Assign each PENDING order to the most suitable AVAILABLE agent.
                Consider the following when assigning:
                - Prefer agents with lower active order count (less workload)
                - Prefer agents with higher rating
                - Every PENDING order must be assigned if a suitable agent exists
                - One agent can receive multiple orders
                
                Return ONLY a valid JSON array. No explanation. No markdown. No extra text.
                
                Format:
                [
                  {
                    "orderId": "ORD-001",
                    "agentId": "AGT-002",
                    "reason": "Lowest workload and highest rating"
                  }
                ]
                
                If an order cannot be assigned, still include it with agentId as null and a reason.
                
                PENDING ORDERS:
                %s
                
                AVAILABLE AGENTS (ID | Name | Active Orders | Rating):
                %s
                """.formatted(ordersContext, agentsContext);
    }

    private AssignOrderResponse parseAssignments(String rawResponse) {
        try {
            String cleaned = rawResponse
                    .replaceAll("```json", "")
                    .replaceAll("```", "")
                    .trim();

            List<?> parsed = objectMapper.readValue(cleaned, List.class);

            List<AssignOrderResponse.AssignmentDetail> assignments = new ArrayList<>();
            int totalAssigned = 0;
            int totalFailed = 0;

            for (Object item : parsed) {
                Map<?, ?> entry = (Map<?, ?>) item;
                String orderId = (String) entry.get("orderId");
                String agentId = (String) entry.get("agentId");
                String reason = (String) entry.get("reason");
                boolean success = agentId != null && !agentId.isBlank();

                if (success) totalAssigned++;
                else totalFailed++;

                assignments.add(AssignOrderResponse.AssignmentDetail.builder()
                        .orderId(orderId)
                        .assignedAgentId(agentId)
                        .success(success)
                        .reason(reason)
                        .build());
            }

            return AssignOrderResponse.builder()
                    .success(totalAssigned > 0)
                    .totalAssigned(totalAssigned)
                    .totalFailed(totalFailed)
                    .assignments(assignments)
                    .message(totalAssigned + " order(s) assigned, " + totalFailed + " could not be assigned.")
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse LLM assignment response: " + e.getMessage(), e);
        }
    }

    private String callOpenAI(String prompt) {
        var body = Map.of(
                "model", model,
                "messages", List.of(Map.of(
                        "role", "user",
                        "content", prompt)),
                "max_tokens", 1000,
                "temperature", 0.2
        );

        var resp = http.post()
                .uri(baseUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + apiKey)
                .body(body)
                .retrieve()
                .body(Map.class);

        log.info("OpenAI raw response: {}", resp);

        try {
            var choices = (List<?>) resp.get("choices");
            var message = (Map<?, ?>) ((Map<?, ?>) choices.get(0)).get("message");
            return (String) message.get("content");
        } catch (Exception e) {
            throw new RuntimeException("OpenAI response parse failed", e);
        }
    }
    private AssignOrderResponse assignUsingFallbackRules(
            String ordersContext,
            String agentsContext) {

        List<AssignOrderResponse.AssignmentDetail> assignments =
                new ArrayList<>();

        try {

            List<String> orderIds = ordersContext.lines()
                    .map(String::trim)
                    .filter(line -> !line.isBlank())
                    .map(line -> line.split("\\|")[0].trim())
                    .toList();

            class AgentInfo {
                String id;
                String name;
                int activeOrders;
                double rating;

                AgentInfo(String id,
                          String name,
                          int activeOrders,
                          double rating) {

                    this.id = id;
                    this.name = name;
                    this.activeOrders = activeOrders;
                    this.rating = rating;
                }
            }

            List<AgentInfo> agents = new ArrayList<>();

            for (String line : agentsContext.split("\\R")) {

                if (line.isBlank()) {
                    continue;
                }

                String[] parts = line.split("\\|");

                if (parts.length < 4) {
                    continue;
                }

                agents.add(
                        new AgentInfo(
                                parts[0].trim(),
                                parts[1].trim(),
                                Integer.parseInt(parts[2].trim()),
                                Double.parseDouble(parts[3].trim())
                        )
                );
            }

            int assigned = 0;
            int failed = 0;

            for (String orderId : orderIds) {

                AgentInfo selectedAgent = agents.stream()
                        .sorted(
                                Comparator
                                        .comparingInt((AgentInfo a) -> a.activeOrders)
                                        .thenComparing(
                                                (AgentInfo a) -> a.rating,
                                                Comparator.reverseOrder()
                                        )
                        )
                        .findFirst()
                        .orElse(null);

                if (selectedAgent == null) {

                    failed++;

                    assignments.add(
                            AssignOrderResponse.AssignmentDetail.builder()
                                    .orderId(orderId)
                                    .success(false)
                                    .reason("No available agent found")
                                    .build()
                    );

                    continue;
                }

                assignments.add(
                        AssignOrderResponse.AssignmentDetail.builder()
                                .orderId(orderId)
                                .assignedAgentId(selectedAgent.id)
                                .agentName(selectedAgent.name)
                                .success(true)
                                .reason(
                                        "Fallback assignment based on lowest active orders and highest rating"
                                )
                                .build()
                );

                selectedAgent.activeOrders++;
                assigned++;
            }

            return AssignOrderResponse.builder()
                    .success(assigned > 0)
                    .totalAssigned(assigned)
                    .totalFailed(failed)
                    .assignments(assignments)
                    .message("Fallback assignment completed.")
                    .build();

        } catch (Exception ex) {

            log.error("Fallback assignment failed", ex);

            return AssignOrderResponse.builder()
                    .success(false)
                    .totalAssigned(0)
                    .totalFailed(0)
                    .assignments(List.of())
                    .message("Fallback assignment failed: " + ex.getMessage())
                    .build();
        }
    }

}