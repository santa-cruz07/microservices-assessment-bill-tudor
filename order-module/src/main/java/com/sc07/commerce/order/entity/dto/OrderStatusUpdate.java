package com.sc07.commerce.order.entity.dto;

import com.sc07.commerce.order.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record OrderStatusUpdate(@NotNull OrderStatus status) {
}
