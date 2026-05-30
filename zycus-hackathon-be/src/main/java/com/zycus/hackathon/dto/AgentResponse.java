package com.zycus.hackathon.dto;

import com.zycus.hackathon.entity.AgentStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AgentResponse {
    private String id;
    private String name;
    private Integer activeOrderCount;
    private AgentStatus status;
    private Double rating;
}