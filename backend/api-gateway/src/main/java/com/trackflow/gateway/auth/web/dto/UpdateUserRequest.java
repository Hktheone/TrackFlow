package com.trackflow.gateway.auth.web.dto;

import com.trackflow.gateway.auth.domain.RoleName;

/** Both fields optional: send only what changes. */
public record UpdateUserRequest(RoleName role, Boolean enabled) {
}
