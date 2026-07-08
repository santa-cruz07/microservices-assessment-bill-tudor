package com.sc07.commerce.order.entity.dto;

import com.sc07.commerce.order.entity.Order;
import com.sc07.commerce.order.entity.OrderStatus;

import java.time.Instant;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String customerEmail,
        String itemDescription,
        int quantity,
        OrderStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public OrderResponse(Order order) {
        this(
                order.getId(),
                order.getCustomerEmail(),
                order.getItemDescription(),
                order.getQuantity(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

}
