package com.zycus.hackathon.controller;

import com.zycus.hackathon.dto.*;
import com.zycus.hackathon.service.OrderManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderManagementController {

    private final OrderManagementService orderManagementService;

    @GetMapping("/agents")
    public ResponseEntity<List<AgentResponse>> getAllAgents() {
        log.info("Fetching all agents");
        return ResponseEntity.ok(orderManagementService.getAllAgents());
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        log.info("Fetching all orders");
        return ResponseEntity.ok(orderManagementService.getAllOrders());
    }

    @PostMapping("/assign")
    public ResponseEntity<AssignOrderResponse> assignOrders() {
        log.info("Triggering bulk order assignment via LLM");
        AssignOrderResponse response = orderManagementService.assignOrders();
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }

    @PostMapping("/create")
    public ResponseEntity<OrderResponse> createOrder(
            @RequestBody CreateOrderRequest request) {

        log.info("Creating order");

        return ResponseEntity.ok(
                orderManagementService.createOrder(request)
        );
    }
    @PutMapping("/agents/{agentId}/status")
    public ResponseEntity<AgentResponse> updateAgentStatus(
            @PathVariable String agentId,
            @RequestBody UpdateAgentStatusRequest request) {

        log.info("Updating status for agent {}", agentId);

        return ResponseEntity.ok(
                orderManagementService.updateAgentStatus(
                        agentId,
                        request
                )
        );
    }
}