package com.sc07.commerce.notification.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @TenantId
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;


    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public void markSent() {
        status = NotificationStatus.SENT;
    }

    public Notification(UUID eventId, UUID orderId,
                         String subject, String body) {
        this.eventId = eventId;
        this.orderId = orderId;
        this.subject = subject;
        this.body = body;
        this.status = NotificationStatus.PENDING;
    }
}
