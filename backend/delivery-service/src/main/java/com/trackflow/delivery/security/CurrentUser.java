package com.trackflow.delivery.security;

import java.util.List;

import org.springframework.security.oauth2.jwt.Jwt;

import com.trackflow.delivery.domain.Delivery;

/**
 * The caller, as described by their access token, plus the delivery access rules for each role:
 * <ul>
 *   <li>ADMIN: everything</li>
 *   <li>USER: deliveries of orders they placed; may assign a rider to them</li>
 *   <li>RIDER: deliveries assigned to them; may update their status and report location</li>
 * </ul>
 */
public record CurrentUser(String username, List<String> roles) {

    public static CurrentUser from(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        return new CurrentUser(jwt.getSubject(), roles == null ? List.of() : roles);
    }

    public boolean isAdmin() {
        return roles.contains("ADMIN");
    }

    public boolean isUser() {
        return roles.contains("USER");
    }

    public boolean isRider() {
        return roles.contains("RIDER");
    }

    public boolean placed(Delivery delivery) {
        return isUser() && username.equals(delivery.getCustomerUsername());
    }

    public boolean carries(Delivery delivery) {
        return isRider() && delivery.getCourier() != null && username.equals(delivery.getCourier().getUsername());
    }

    public boolean canView(Delivery delivery) {
        return isAdmin() || placed(delivery) || carries(delivery);
    }

    public boolean canAssign(Delivery delivery) {
        return isAdmin() || placed(delivery);
    }

    /** Status changes and location pings come from the rider on the job (or an admin). */
    public boolean canDrive(Delivery delivery) {
        return isAdmin() || carries(delivery);
    }
}
