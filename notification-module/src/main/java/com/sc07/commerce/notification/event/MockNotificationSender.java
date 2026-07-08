package com.sc07.commerce.notification.event;

import com.sc07.commerce.notification.entity.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MockNotificationSender implements NotificationSender {

    @Override
    public void send(Notification notification) {
      log.info("Sending notification: {}", notification);
    }
}
