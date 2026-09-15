package com.tbm.recruitment.notification.dto.response;

import com.tbm.recruitment.notification.enums.NotificationType;
import java.util.UUID;

public record NotificationCreatedSseEvent(
    String type, UUID notificationId, NotificationType notificationType, UUID referenceId) {}
