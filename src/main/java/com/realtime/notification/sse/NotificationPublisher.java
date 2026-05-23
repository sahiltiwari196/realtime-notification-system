package com.realtime.notification.sse;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationPublisher {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public static final String CHANNEL = "notifications";

    public void publish(Long userId, Object payload) {
        try {
            String message = userId + "::" + objectMapper.writeValueAsString(payload);
            redisTemplate.convertAndSend(CHANNEL, message);
        } catch (Exception e) {
            log.error("Failed to publish notification to Redis: {}", e.getMessage());
        }
    }
}