package com.sc07.commerce.order.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderStatusTest {

    @Test
    void allowsOnlyConfiguredForwardTransitions() {
        assertThat(OrderStatus.PENDING.canTransition(OrderStatus.PROCESSING)).isTrue();
        assertThat(OrderStatus.PENDING.canTransition(OrderStatus.CANCELLED)).isTrue();
        assertThat(OrderStatus.PROCESSING.canTransition(OrderStatus.SHIPPED)).isTrue();
        assertThat(OrderStatus.PROCESSING.canTransition(OrderStatus.CANCELLED)).isTrue();
        assertThat(OrderStatus.SHIPPED.canTransition(OrderStatus.DELIVERED)).isTrue();

        assertThat(OrderStatus.PENDING.canTransition(OrderStatus.SHIPPED)).isFalse();
        assertThat(OrderStatus.PROCESSING.canTransition(OrderStatus.DELIVERED)).isFalse();
        assertThat(OrderStatus.SHIPPED.canTransition(OrderStatus.CANCELLED)).isFalse();
    }

    @Test
    void marksDeliveredAndCancelledAsTerminal() {
        assertThat(OrderStatus.PENDING.isTerminal()).isFalse();
        assertThat(OrderStatus.PROCESSING.isTerminal()).isFalse();
        assertThat(OrderStatus.SHIPPED.isTerminal()).isFalse();
        assertThat(OrderStatus.DELIVERED.isTerminal()).isTrue();
        assertThat(OrderStatus.CANCELLED.isTerminal()).isTrue();
    }
}
