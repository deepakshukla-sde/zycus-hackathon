package com.zycus.hackathon.service;

import com.zycus.hackathon.dto.*;

import java.util.List;

public interface OrderManagementService {
    List<AgentResponse> getAllAgents();
    List<OrderResponse> getAllOrders();
    AssignOrderResponse assignOrders();
    OrderResponse createOrder(CreateOrderRequest request);
    AgentResponse updateAgentStatus(String agentId, UpdateAgentStatusRequest request);
}