package com.trackflow.gateway.auth.web.dto;

import java.time.Instant;
import java.util.UUID;

import com.trackflow.gateway.auth.domain.AppUser;
import com.trackflow.gateway.auth.domain.RoleName;

public record UserResponse(UUID id, String username, String displayName, RoleName role, boolean enabled,
                           Instant createdAt) {

    public static UserResponse from(AppUser user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getDisplayName(), user.getRoleName(),
                user.isEnabled(), user.getCreatedAt());
    }
}
