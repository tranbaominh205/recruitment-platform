package com.tbm.recruitment.notification.dto.response;

import com.tbm.recruitment.notification.enums.NotificationType;
import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
    UUID id,
    NotificationType type,
    String title,
    String message,
    UUID referenceId,
    boolean read,
    Instant createdAt) {}
