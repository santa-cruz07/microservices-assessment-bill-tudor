package com.sc07.commerce.notification.service;

import com.sc07.commerce.notification.entity.Notification;
import com.sc07.commerce.notification.event.NotificationSender;
import com.sc07.commerce.notification.repository.NotificationRepository;
import com.sc07.commerce.shared.v1.OrderEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Service
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationSender notificationSender;

    public NotificationService(NotificationRepository notificationRepository, NotificationSender notificationSender) {
        this.notificationRepository = notificationRepository;
        this.notificationSender = notificationSender;
    }

    public List<Notification> getAllOrdered() {
        return notificationRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public void handle(OrderEvent event) {
        Notification notification = buildFrom(event);
        try {
            notificationRepository.saveAndFlush(notification);
        } catch (DataIntegrityViolationException e) {
            log.info("Duplicate delivery of event {} ignored (already processed)", event.eventId());
            return;
        }
        notificationSender.send(notification);
        notification.markSent();
    }


    Notification buildFrom(OrderEvent event) {
        OrderEvent.Payload p = event.payload();
        String subject;
        String body;
        switch (event.eventType()) {
            case ORDER_CREATED -> {
                subject = "Order received";
                body = "Order %s received.".formatted(p.orderId());
            }
            case ORDER_CANCELLED -> {
                subject = "Order cancelled";
                body = "Order %s cancelled.".formatted(p.orderId());
            }
            case ORDER_STATUS_CHANGED -> {
                if ("DELIVERED".equals(p.newStatus())) {
                    subject = "Order delivered";
                    body = "Order %s delivered.".formatted(p.orderId());
                } else {
                    subject = "Order update: " + p.newStatus();
                    body = "Order %s status: %s.".formatted(p.orderId(), p.newStatus());
                }
            }
            default -> throw new IllegalArgumentException("Unknown event type: " + event.eventType());
        }
        return new Notification(event.eventId(),p.orderId(),subject,body);

    }
}
