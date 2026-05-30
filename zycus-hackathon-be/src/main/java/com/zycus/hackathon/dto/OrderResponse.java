package com.zycus.hackathon.dto;

import com.zycus.hackathon.entity.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class OrderResponse {
    private String id;
    private String description;
    private String assignedAgentId;
    private String assignedAgentName;
    private OrderStatus status;
    private LocalDateTime createdAt;
}