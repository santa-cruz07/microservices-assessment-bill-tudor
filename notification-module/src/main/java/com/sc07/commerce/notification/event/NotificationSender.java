package com.sc07.commerce.notification.event;

import com.sc07.commerce.notification.entity.Notification;

public interface NotificationSender {

    void send(Notification notification);
}
