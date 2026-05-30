package com.zycus.hackathon.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "zycus_hack_agents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Agent {

    @Id
    @Column(name = "id", length = 20)
    private String id;

    @Column(name = "name", length = 120, nullable = false)
    private String name;

    @Column(name = "active_order_count", nullable = false)
    private Integer activeOrderCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AgentStatus status;

    @Column(name = "rating")
    private Double rating;
}