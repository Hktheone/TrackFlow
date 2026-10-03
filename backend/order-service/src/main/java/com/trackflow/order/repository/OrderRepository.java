package com.trackflow.order.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.trackflow.order.domain.Order;
import com.trackflow.order.domain.OrderStatus;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    @EntityGraph(attributePaths = "items")
    List<Order> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "items")
    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    @EntityGraph(attributePaths = "items")
    List<Order> findByCustomerUsernameOrderByCreatedAtDesc(String customerUsername);

    @EntityGraph(attributePaths = "items")
    List<Order> findByCustomerUsernameAndStatusOrderByCreatedAtDesc(String customerUsername, OrderStatus status);

    boolean existsByOrderNumber(String orderNumber);
}
