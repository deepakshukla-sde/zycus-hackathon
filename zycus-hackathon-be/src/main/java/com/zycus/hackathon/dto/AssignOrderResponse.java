package com.zycus.hackathon.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AssignOrderResponse {
    private boolean success;
    private int totalAssigned;
    private int totalFailed;
    private List<AssignmentDetail> assignments;
    private String message;

    @Data
    @Builder
    public static class AssignmentDetail {
        private String orderId;
        private String assignedAgentId;
        private String agentName;
        private boolean success;
        private String reason;
    }
}