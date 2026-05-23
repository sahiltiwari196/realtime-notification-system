package com.realtime.notification.sse;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationSubscriber implements MessageListener {

    private final SseConnectionManager sseConnectionManager;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody());
            // Format: "userId::jsonPayload"
            int separatorIndex = body.indexOf("::");
            Long userId = Long.parseLong(body.substring(0, separatorIndex));
            String json = body.substring(separatorIndex + 2);
            Object payload = objectMapper.readValue(json, Object.class);
            sseConnectionManager.sendToUser(userId, payload);
        } catch (Exception e) {
            log.error("Failed to process Redis message: {}", e.getMessage());
        }
    }
}