package com.sc07.commerce.order.exception;

import com.sc07.commerce.order.entity.OrderStatus;

import java.util.UUID;

public class IllegalOrderStatusException extends RuntimeException {
    public IllegalOrderStatusException(UUID orderId, OrderStatus from, OrderStatus to) {
        super("Order %s cannot transition from %s to %s".formatted(orderId, from, to));
    }
}
