package com.trackflow.order.security;

import java.util.List;

import org.springframework.security.oauth2.jwt.Jwt;

/** The caller, as described by their access token: {@code sub} is the username, {@code roles} their role. */
public record CurrentUser(String username, List<String> roles) {

    public static CurrentUser from(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        return new CurrentUser(jwt.getSubject(), roles == null ? List.of() : roles);
    }

    public boolean isAdmin() {
        return roles.contains("ADMIN");
    }

    /** Admins can act on any order; everyone else only on their own. */
    public boolean canAccessOrderOf(String ownerUsername) {
        return isAdmin() || username.equals(ownerUsername);
    }
}
