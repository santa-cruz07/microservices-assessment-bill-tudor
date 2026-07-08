package com.sc07.commerce.shared.v1;

import java.time.Instant;
import java.util.UUID;

public record OrderEvent(
        UUID eventId,
        OrderEventType eventType,
        int eventVersion,
        UUID tenantId,
        Instant occurredAt,
        Payload payload) {

    public static final int CURRENT_VERSION = 1;

    public record Payload(
            UUID orderId,
            String customerName,
            String customerEmail,
            String oldStatus,
            String newStatus) {
    }
}

