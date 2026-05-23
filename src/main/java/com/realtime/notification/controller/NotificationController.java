package com.realtime.notification.controller;


import com.realtime.notification.dto.NotificationRequest;
import com.realtime.notification.entity.Notification;
import com.realtime.notification.ratelimit.RateLimit;
import com.realtime.notification.service.NotificationService;
import com.realtime.notification.sse.NotificationPublisher;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NotificationController {

    private final NotificationService notificationService;

    private final NotificationPublisher notificationPublisher;

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Notification>> getUserNotifications(@PathVariable Long userId) {
        return ResponseEntity.ok(notificationService.getUserNotifications(userId));
    }

    @GetMapping("/user/{userId}/unread-count")
    public ResponseEntity<Long> getUnreadCount(@PathVariable Long userId) {
        return ResponseEntity.ok(notificationService.getUnreadCount(userId));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Notification> markAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.markAsRead(id));
    }
    @PostMapping
    @RateLimit(maxRequests = 10, windowSeconds = 60)
    public ResponseEntity<Notification> createNotification(@RequestBody NotificationRequest request) {
        Notification notification = notificationService.createNotification(request);
        notificationPublisher.publish(notification.getUserId(), notification);

        return ResponseEntity.ok(notification);
    }
}
