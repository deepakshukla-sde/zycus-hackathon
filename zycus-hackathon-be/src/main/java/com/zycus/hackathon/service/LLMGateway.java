package com.zycus.hackathon.service;

import com.zycus.hackathon.dto.AssignOrderResponse;

public interface LLMGateway {
    AssignOrderResponse assignOrders(String ordersContext, String agentsContext);
}