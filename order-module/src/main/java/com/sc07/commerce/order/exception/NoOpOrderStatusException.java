package com.sc07.commerce.order.exception;

import com.sc07.commerce.order.entity.OrderStatus;

import java.util.UUID;

public class NoOpOrderStatusException extends RuntimeException {
    public NoOpOrderStatusException(UUID orderId, OrderStatus status) {
        super("Order %s is already status: %s".formatted(orderId,status));
    }
}
