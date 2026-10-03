package com.trackflow.order.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

class OrderTest {

    private Order newOrder() {
        return new Order("TF-TEST01", "ada", "Ada", null, "1 Main St", List.of(
                new OrderItem("Widget", 2, new BigDecimal("4.50")),
                new OrderItem("Gadget", 1, new BigDecimal("10.00"))));
    }

    @Test
    void totalIsSumOfLineTotalsAndStartsPending() {
        Order order = newOrder();

        assertThat(order.getTotalAmount()).isEqualByComparingTo("19.00");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void followsHappyPathThroughDelivery() {
        Order order = newOrder();

        order.transitionTo(OrderStatus.CONFIRMED);
        order.transitionTo(OrderStatus.COURIER_ASSIGNED);
        order.transitionTo(OrderStatus.PICKED_UP);
        order.transitionTo(OrderStatus.IN_TRANSIT);
        order.transitionTo(OrderStatus.DELIVERED);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(order.getStatus().isTerminal()).isTrue();
    }

    @Test
    void canSkipForwardButNeverBackwards() {
        Order order = newOrder();
        order.transitionTo(OrderStatus.CONFIRMED);
        order.transitionTo(OrderStatus.IN_TRANSIT);

        assertThatThrownBy(() -> order.transitionTo(OrderStatus.COURIER_ASSIGNED))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void cannotCancelOncePickedUp() {
        Order order = newOrder();
        order.transitionTo(OrderStatus.CONFIRMED);
        order.transitionTo(OrderStatus.PICKED_UP);

        assertThatThrownBy(() -> order.transitionTo(OrderStatus.CANCELLED))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void pendingOrderCannotJumpStraightToDelivery() {
        assertThatThrownBy(() -> newOrder().transitionTo(OrderStatus.COURIER_ASSIGNED))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void rejectsEmptyOrders() {
        assertThatThrownBy(() -> new Order("TF-X", "ada", "Ada", null, "1 Main St", List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
