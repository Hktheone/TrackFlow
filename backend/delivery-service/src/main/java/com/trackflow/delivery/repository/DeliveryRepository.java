package com.trackflow.delivery.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.trackflow.delivery.domain.Delivery;
import com.trackflow.delivery.domain.DeliveryStatus;

public interface DeliveryRepository extends JpaRepository<Delivery, UUID> {

    Optional<Delivery> findByOrderId(UUID orderId);

    boolean existsByOrderId(UUID orderId);

    List<Delivery> findAllByOrderByCreatedAtDesc();

    List<Delivery> findByStatusOrderByCreatedAtDesc(DeliveryStatus status);

    List<Delivery> findByCustomerUsernameOrderByCreatedAtDesc(String customerUsername);

    List<Delivery> findByCourierUsernameOrderByCreatedAtDesc(String courierUsername);
}
