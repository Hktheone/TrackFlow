package com.trackflow.delivery.web.dto;

import com.trackflow.delivery.domain.DeliveryStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateStatusRequest(@NotNull DeliveryStatus status, @Size(max = 300) String note) {
}
