package com.realtime.notification.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@Slf4j
public class SseConnectionManager {

    // One user can have multiple devices/connections
    private final Map<Long, List<SseEmitter>> userEmitters = new ConcurrentHashMap<>();

    public SseEmitter addEmitter(Long userId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        userEmitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, emitter));
        emitter.onError(e -> removeEmitter(userId, emitter));

        log.info("SSE connection added for userId: {}", userId);
        return emitter;
    }

    public void sendToUser(Long userId, Object data) {
        List<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters == null || emitters.isEmpty()) return;

        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();

        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().name("notification").data(data));
            } catch (IOException e) {
                deadEmitters.add(emitter);
                log.warn("Dead SSE emitter removed for userId: {}", userId);
            }
        });

        emitters.removeAll(deadEmitters);
    }

    private void removeEmitter(Long userId, SseEmitter emitter) {
        List<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) userEmitters.remove(userId);
        }
        log.info("SSE connection removed for userId: {}", userId);
    }
}