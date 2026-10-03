package com.trackflow.delivery.web.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record AssignCourierRequest(@NotNull UUID courierId) {
}
