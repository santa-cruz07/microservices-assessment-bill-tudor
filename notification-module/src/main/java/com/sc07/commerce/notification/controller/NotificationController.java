package com.sc07.commerce.notification.controller;

import com.sc07.commerce.notification.entity.dto.NotificationResponse;
import com.sc07.commerce.notification.repository.NotificationRepository;
import com.sc07.commerce.notification.service.NotificationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationResponse> list() {
        return notificationService.getAllOrdered().stream().map(NotificationResponse::new).toList();
    }
}