package com.trackflow.delivery.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.trackflow.delivery.domain.Courier;
import com.trackflow.delivery.domain.Delivery;
import com.trackflow.delivery.domain.VehicleType;

class CurrentUserTest {

    private static final CurrentUser ADMIN = new CurrentUser("admin", List.of("ADMIN"));
    private static final CurrentUser GRACE = new CurrentUser("grace", List.of("USER"));
    private static final CurrentUser ALAN = new CurrentUser("alan", List.of("USER"));
    private static final CurrentUser AMARA = new CurrentUser("amara", List.of("RIDER"));
    private static final CurrentUser BILAL = new CurrentUser("bilal", List.of("RIDER"));

    private Delivery graceDeliveryCarriedByAmara() {
        Delivery delivery = new Delivery(UUID.randomUUID(), "TF-1", "grace", "Grace", null, "1 Main St");
        delivery.assign(new Courier("Amara", "1", VehicleType.SCOOTER, "amara"));
        return delivery;
    }

    @Test
    void adminCanDoEverything() {
        Delivery delivery = graceDeliveryCarriedByAmara();

        assertThat(ADMIN.canView(delivery)).isTrue();
        assertThat(ADMIN.canAssign(delivery)).isTrue();
        assertThat(ADMIN.canDrive(delivery)).isTrue();
    }

    @Test
    void customerManagesOnlyTheirOwnDelivery() {
        Delivery delivery = graceDeliveryCarriedByAmara();

        assertThat(GRACE.canView(delivery)).isTrue();
        assertThat(GRACE.canAssign(delivery)).isTrue();
        assertThat(GRACE.canDrive(delivery)).isFalse();

        assertThat(ALAN.canView(delivery)).isFalse();
        assertThat(ALAN.canAssign(delivery)).isFalse();
    }

    @Test
    void riderDrivesOnlyWhatIsAssignedToThem() {
        Delivery delivery = graceDeliveryCarriedByAmara();

        assertThat(AMARA.canView(delivery)).isTrue();
        assertThat(AMARA.canDrive(delivery)).isTrue();
        assertThat(AMARA.canAssign(delivery)).isFalse();

        assertThat(BILAL.canView(delivery)).isFalse();
        assertThat(BILAL.canDrive(delivery)).isFalse();
    }

    @Test
    void riderWithTheCustomersUsernameStillIsNotTheCustomer() {
        // Role matters, not just the name: a RIDER named "grace" must not inherit the customer's rights.
        CurrentUser riderGrace = new CurrentUser("grace", List.of("RIDER"));

        assertThat(riderGrace.canAssign(graceDeliveryCarriedByAmara())).isFalse();
    }
}
