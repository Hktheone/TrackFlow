package com.trackflow.order.web.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record CreateOrderRequest(
        @NotBlank @Size(max = 120) String customerName,
        @Size(max = 30) String customerPhone,
        @NotBlank @Size(max = 300) String deliveryAddress,
        @NotEmpty @Size(max = 50) List<@Valid OrderItemRequest> items) {
}
