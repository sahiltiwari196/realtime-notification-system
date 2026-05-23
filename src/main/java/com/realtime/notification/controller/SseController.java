package com.realtime.notification.controller;

import com.realtime.notification.service.NotificationService;
import com.realtime.notification.sse.SseConnectionManager;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SseController {

    private final SseConnectionManager sseConnectionManager;
    private final NotificationService notificationService;

    @GetMapping("/subscribe/{userId}")
    public SseEmitter subscribe(@PathVariable Long userId) {
        SseEmitter emitter = sseConnectionManager.addEmitter(userId);

        // Push unread notifications on reconnect (retry mechanism)
        notificationService.getUserNotifications(userId).stream()
            .filter(n -> !n.isRead())
            .forEach(n -> sseConnectionManager.sendToUser(userId, n));

        return emitter;
    }
}