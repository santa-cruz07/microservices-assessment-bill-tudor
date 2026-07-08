package com.sc07.commerce.notification.entity.dto;

import com.sc07.commerce.notification.entity.Notification;
import com.sc07.commerce.notification.entity.NotificationStatus;


import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID orderId,
        String subject,
        String body,
        NotificationStatus status,
        Instant createdAt) {

    public NotificationResponse(Notification notification) {
        this(
                notification.getId(),
                notification.getOrderId(),
                notification.getSubject(),
                notification.getBody(),
                notification.getStatus(),
                notification.getCreatedAt()
        );

    }

}
