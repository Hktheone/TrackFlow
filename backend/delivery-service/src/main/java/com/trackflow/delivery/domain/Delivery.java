package com.trackflow.delivery.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * A delivery is created from an ORDER_CONFIRMED event and holds a snapshot of the order details
 * it needs, so delivery-service never has to call order-service synchronously.
 */
@Entity
@Table(name = "deliveries")
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Column(name = "order_number", nullable = false, length = 20)
    private String orderNumber;

    /** Username of the customer who placed the order. */
    @Column(name = "customer_username", length = 50)
    private String customerUsername;

    @Column(name = "customer_name", nullable = false, length = 120)
    private String customerName;

    @Column(name = "customer_phone", length = 30)
    private String customerPhone;

    @Column(name = "delivery_address", nullable = false, length = 300)
    private String deliveryAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DeliveryStatus status;

    @ManyToOne
    @JoinColumn(name = "courier_id")
    private Courier courier;

    @Column(length = 300)
    private String note;

    @Version
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "assigned_at")
    private Instant assignedAt;

    @Column(name = "picked_up_at")
    private Instant pickedUpAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    protected Delivery() {
    }

    public Delivery(UUID orderId, String orderNumber, String customerUsername, String customerName,
                    String customerPhone, String deliveryAddress) {
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.customerUsername = customerUsername;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.deliveryAddress = deliveryAddress;
        this.status = DeliveryStatus.PENDING_ASSIGNMENT;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    /** Assigns (or, before pickup, reassigns) a courier, freeing up any previous one. */
    public void assign(Courier next) {
        if (!status.isBeforePickup()) {
            throw new InvalidDeliveryTransitionException("Courier can only be changed before pickup (delivery is " + status + ")");
        }
        if (!next.isAvailable()) {
            throw new InvalidDeliveryTransitionException("Courier " + next.getName() + " is already on a delivery");
        }
        if (courier != null) {
            courier.release();
        }
        next.markAssigned();
        this.courier = next;
        this.status = DeliveryStatus.ASSIGNED;
        this.assignedAt = Instant.now();
        this.updatedAt = this.assignedAt;
    }

    public void transitionTo(DeliveryStatus next, String note) {
        if (!status.canTransitionTo(next)) {
            throw new InvalidDeliveryTransitionException(status, next);
        }
        Instant now = Instant.now();
        this.status = next;
        this.updatedAt = now;
        if (note != null && !note.isBlank()) {
            this.note = note;
        }
        if (next == DeliveryStatus.PICKED_UP) {
            this.pickedUpAt = now;
        }
        if (next == DeliveryStatus.DELIVERED) {
            this.deliveredAt = now;
        }
        if (next.isTerminal() && courier != null) {
            courier.release();
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public String getCustomerUsername() {
        return customerUsername;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public DeliveryStatus getStatus() {
        return status;
    }

    public Courier getCourier() {
        return courier;
    }

    public String getNote() {
        return note;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }

    public Instant getPickedUpAt() {
        return pickedUpAt;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }
}
