package com.zycus.hackathon.repository;

import com.zycus.hackathon.entity.Order;
import com.zycus.hackathon.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, String> {
    List<Order> findByStatus(OrderStatus status);
    List<Order> findByAssignedAgentId(String agentId);
    List<Order> findByStatusAndAssignedAgentIdIn(OrderStatus status, List<String> agentIds);
}