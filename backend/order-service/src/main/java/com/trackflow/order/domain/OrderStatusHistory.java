package com.trackflow.order.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Append-only audit trail of every status an order has been in. */
@Entity
@Table(name = "order_status_history")
public class OrderStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusChangeSource source;

    @Column(length = 300)
    private String note;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    protected OrderStatusHistory() {
    }

    public OrderStatusHistory(UUID orderId, OrderStatus status, StatusChangeSource source, String note) {
        this.orderId = orderId;
        this.status = status;
        this.source = source;
        this.note = note;
        this.changedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public StatusChangeSource getSource() {
        return source;
    }

    public String getNote() {
        return note;
    }

    public Instant getChangedAt() {
        return changedAt;
    }
}
