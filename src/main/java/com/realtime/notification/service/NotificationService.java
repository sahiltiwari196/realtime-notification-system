package com.realtime.notification.service;

import com.realtime.notification.dto.NotificationRequest;
import com.realtime.notification.entity.Notification;
import com.realtime.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
	
    private final NotificationRepository notificationRepository;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String UNREAD_COUNT_KEY = "unread:user:";

    public Notification createNotification(NotificationRequest request) {
        // Duplicate check via idempotency key
        if (request.getIdempotencyKey() != null) {
            notificationRepository.findByIdempotencyKey(request.getIdempotencyKey())
                .ifPresent(existing -> {
                    throw new IllegalStateException("Duplicate notification detected");
                });
        }

        Notification notification = new Notification();
        notification.setUserId(request.getUserId());
        notification.setMessage(request.getMessage());
        notification.setIdempotencyKey(request.getIdempotencyKey());

        Notification saved = notificationRepository.save(notification);

        // Increment unread count in Redis
        try {
            redisTemplate.opsForValue().increment(UNREAD_COUNT_KEY + request.getUserId());
        } catch (Exception e) {
            log.warn("Redis unavailable, skipping unread count increment: {}", e.getMessage());
        }

        return saved;
    }

    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public long getUnreadCount(Long userId) {
        try {
            String count = redisTemplate.opsForValue().get(UNREAD_COUNT_KEY + userId);
            if (count != null) return Long.parseLong(count);
        } catch (Exception e) {
            log.warn("Redis unavailable, falling back to DB for unread count: {}", e.getMessage());
        }
        // Fallback to DB if Redis is down
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    public Notification markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (!notification.isRead()) {
            notification.setRead(true);
            notificationRepository.save(notification);

            // Decrement Redis unread count
            try {
                redisTemplate.opsForValue().decrement(UNREAD_COUNT_KEY + notification.getUserId());
            } catch (Exception e) {
                log.warn("Redis unavailable, skipping unread count decrement: {}", e.getMessage());
            }
        }

        return notification;
    }
}