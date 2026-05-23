package com.realtime.notification.dto;

import lombok.Data;

@Data
public class NotificationRequest {
    private Long userId;
    private String message;
    private String idempotencyKey;
}