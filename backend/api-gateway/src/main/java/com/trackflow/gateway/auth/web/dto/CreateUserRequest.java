package com.trackflow.gateway.auth.web.dto;

import com.trackflow.gateway.auth.domain.RoleName;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9._-]{3,50}", message = "must be 3-50 letters, digits, dots, dashes or underscores")
        String username,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotBlank @Size(max = 100) String displayName,
        @NotNull RoleName role) {
}
