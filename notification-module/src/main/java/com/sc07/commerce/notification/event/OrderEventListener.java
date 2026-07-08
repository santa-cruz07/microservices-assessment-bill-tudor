package com.sc07.commerce.notification.event;

import com.sc07.commerce.notification.configuration.ApiKeyAuthToken;
import com.sc07.commerce.notification.configuration.RabbitConfig;
import com.sc07.commerce.notification.configuration.TenantPrincipal;
import com.sc07.commerce.notification.service.NotificationService;
import com.sc07.commerce.shared.v1.OrderEvent;
import com.sc07.commerce.shared.v1.OrderEvents;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class OrderEventListener {

    private final NotificationService notificationService;

    public OrderEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE)
    public void onOrderEvent(
            OrderEvent event,
            @Header(name = OrderEvents.TENANT_HEADER, required = false) UUID tenantHeader
    ) {
        UUID tenantId = tenantHeader != null
                ? tenantHeader
                : event.tenantId();

        TenantPrincipal principal = new TenantPrincipal(
                tenantId
        );

        SecurityContextHolder.getContext()
                .setAuthentication(new ApiKeyAuthToken(principal));

        try {
            notificationService.handle(event);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
