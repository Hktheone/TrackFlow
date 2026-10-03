package com.trackflow.order.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.trackflow.order.domain.Order;
import com.trackflow.order.domain.OrderStatus;

public record OrderResponse(
        UUID id,
        String orderNumber,
        String customerUsername,
        String customerName,
        String customerPhone,
        String deliveryAddress,
        List<Item> items,
        BigDecimal totalAmount,
        OrderStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public record Item(String productName, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
    }

    public static OrderResponse from(Order order) {
        List<Item> items = order.getItems().stream()
                .map(i -> new Item(i.getProductName(), i.getQuantity(), i.getUnitPrice(), i.lineTotal()))
                .toList();
        return new OrderResponse(order.getId(), order.getOrderNumber(), order.getCustomerUsername(), order.getCustomerName(),
                order.getCustomerPhone(), order.getDeliveryAddress(), items, order.getTotalAmount(),
                order.getStatus(), order.getCreatedAt(), order.getUpdatedAt());
    }
}
