package com.zycus.hackathon.dto;

import com.zycus.hackathon.entity.AgentStatus;
import lombok.Data;

@Data
public class UpdateAgentStatusRequest {
    private AgentStatus status;
}