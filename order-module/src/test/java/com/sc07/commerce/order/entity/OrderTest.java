package com.sc07.commerce.order.entity;

import com.sc07.commerce.order.entity.dto.OrderCreate;
import com.sc07.commerce.order.exception.IllegalOrderStatusException;
import com.sc07.commerce.order.exception.NoOpOrderStatusException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    @Test
    void constructsPendingOrderFromCreateRequest() {
        Order order = new Order(new OrderCreate("buyer@example.com", "Laptop", 2));

        assertThat(order.getCustomerEmail()).isEqualTo("buyer@example.com");
        assertThat(order.getItemDescription()).isEqualTo("Laptop");
        assertThat(order.getQuantity()).isEqualTo(2);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void updateStatusReturnsPreviousStatusAndChangesOrder() {
        Order order = orderWithStatus(OrderStatus.PENDING);

        OrderStatus previous = order.updateStatus(OrderStatus.PROCESSING);

        assertThat(previous).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PROCESSING);
    }

    @Test
    void updateStatusRejectsNoOpTransition() {
        Order order = orderWithStatus(OrderStatus.PENDING);

        assertThatThrownBy(() -> order.updateStatus(OrderStatus.PENDING))
                .isInstanceOf(NoOpOrderStatusException.class)
                .hasMessageContaining("already status: PENDING");
    }

    @Test
    void updateStatusRejectsIllegalTransition() {
        Order order = orderWithStatus(OrderStatus.SHIPPED);

        assertThatThrownBy(() -> order.updateStatus(OrderStatus.CANCELLED))
                .isInstanceOf(IllegalOrderStatusException.class)
                .hasMessageContaining("cannot transition from SHIPPED to CANCELLED");
    }

    private static Order orderWithStatus(OrderStatus status) {
        Order order = new Order(new OrderCreate("buyer@example.com", "Laptop", 2));
        order.setId(UUID.randomUUID());
        order.setStatus(status);
        return order;
    }
}
