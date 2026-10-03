package com.trackflow.delivery.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class DeliveryTest {

    private Delivery newDelivery() {
        return new Delivery(UUID.randomUUID(), "TF-TEST01", "ada", "Ada", null, "1 Main St");
    }

    @Test
    void startsUnassigned() {
        assertThat(newDelivery().getStatus()).isEqualTo(DeliveryStatus.PENDING_ASSIGNMENT);
    }

    @Test
    void assigningTakesCourierOffTheMarketUntilDelivered() {
        Delivery delivery = newDelivery();
        Courier courier = new Courier("Bo", "123", VehicleType.BICYCLE, "bo");

        delivery.assign(courier);
        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.ASSIGNED);
        assertThat(courier.isAvailable()).isFalse();

        delivery.transitionTo(DeliveryStatus.PICKED_UP, null);
        delivery.transitionTo(DeliveryStatus.IN_TRANSIT, null);
        delivery.transitionTo(DeliveryStatus.DELIVERED, "Left with neighbour");

        assertThat(courier.isAvailable()).isTrue();
        assertThat(delivery.getDeliveredAt()).isNotNull();
        assertThat(delivery.getNote()).isEqualTo("Left with neighbour");
    }

    @Test
    void reassignmentFreesThePreviousCourier() {
        Delivery delivery = newDelivery();
        Courier first = new Courier("Bo", "123", VehicleType.BICYCLE, "bo");
        Courier second = new Courier("Cy", "456", VehicleType.CAR, "cy");

        delivery.assign(first);
        delivery.assign(second);

        assertThat(first.isAvailable()).isTrue();
        assertThat(second.isAvailable()).isFalse();
        assertThat(delivery.getCourier()).isSameAs(second);
    }

    @Test
    void cannotAssignABusyCourier() {
        Courier busy = new Courier("Bo", "123", VehicleType.BICYCLE, "bo");
        newDelivery().assign(busy);

        assertThatThrownBy(() -> newDelivery().assign(busy))
                .isInstanceOf(InvalidDeliveryTransitionException.class);
    }

    @Test
    void cannotReassignAfterPickup() {
        Delivery delivery = newDelivery();
        delivery.assign(new Courier("Bo", "123", VehicleType.BICYCLE, "bo"));
        delivery.transitionTo(DeliveryStatus.PICKED_UP, null);

        assertThatThrownBy(() -> delivery.assign(new Courier("Cy", "456", VehicleType.CAR, "cy")))
                .isInstanceOf(InvalidDeliveryTransitionException.class);
    }

    @Test
    void cannotPickUpWithoutACourier() {
        assertThatThrownBy(() -> newDelivery().transitionTo(DeliveryStatus.PICKED_UP, null))
                .isInstanceOf(InvalidDeliveryTransitionException.class);
    }
}
