package com.trackflow.delivery.web.dto;

import com.trackflow.delivery.domain.VehicleType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCourierRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 30) String phone,
        @NotNull VehicleType vehicleType,
        @NotBlank @Pattern(regexp = "[A-Za-z0-9._-]{3,50}", message = "must be the rider's login username")
        String username) {
}
