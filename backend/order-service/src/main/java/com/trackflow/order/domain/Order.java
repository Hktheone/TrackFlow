package com.trackflow.order.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_number", nullable = false, unique = true, length = 20)
    private String orderNumber;

    /** Username of the account that placed the order. */
    @Column(name = "customer_username", length = 50)
    private String customerUsername;

    @Column(name = "customer_name", nullable = false, length = 120)
    private String customerName;

    @Column(name = "customer_phone", length = 30)
    private String customerPhone;

    @Column(name = "delivery_address", nullable = false, length = 300)
    private String deliveryAddress;

    @ElementCollection
    @CollectionTable(name = "order_items", joinColumns = @JoinColumn(name = "order_id"))
    @OrderColumn(name = "line_no")
    private List<OrderItem> items = new ArrayList<>();

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    @Version
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Order() {
    }

    public Order(String orderNumber, String customerUsername, String customerName, String customerPhone,
                 String deliveryAddress, List<OrderItem> items) {
        if (items.isEmpty()) {
            throw new IllegalArgumentException("An order needs at least one item");
        }
        this.orderNumber = orderNumber;
        this.customerUsername = customerUsername;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.deliveryAddress = deliveryAddress;
        this.items.addAll(items);
        this.totalAmount = items.stream().map(OrderItem::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        this.status = OrderStatus.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    /**
     * Moves the order to {@code next}.
     *
     * @throws InvalidStatusTransitionException if the lifecycle does not allow it
     */
    public void transitionTo(OrderStatus next) {
        if (!status.canTransitionTo(next)) {
            throw new InvalidStatusTransitionException(status, next);
        }
        this.status = next;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
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

    public List<OrderItem> getItems() {
        return List.copyOf(items);
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
