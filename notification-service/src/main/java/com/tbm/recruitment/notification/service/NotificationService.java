package com.tbm.recruitment.notification.service;

import com.tbm.recruitment.notification.dto.response.NotificationResponse;
import com.tbm.recruitment.notification.dto.response.UnreadNotificationCountResponse;
import com.tbm.recruitment.notification.entity.Notification;
import com.tbm.recruitment.notification.enums.NotificationType;
import com.tbm.recruitment.notification.exception.AppException;
import com.tbm.recruitment.notification.exception.ErrorCode;
import com.tbm.recruitment.notification.mapper.NotificationMapper;
import com.tbm.recruitment.notification.repository.NotificationRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationService {

  NotificationRepository notificationRepository;
  NotificationMapper notificationMapper;
  NotificationSseService notificationSseService;

  public List<NotificationResponse> getMyNotifications(String accountIdHeader, String accountRole) {

    UUID recipientAccountId = requireNotificationRecipient(accountIdHeader, accountRole);

    return notificationRepository
        .findAllByRecipientAccountIdOrderByCreatedAtDesc(recipientAccountId)
        .stream()
        .map(notificationMapper::toNotificationResponse)
        .toList();
  }

  public UnreadNotificationCountResponse getUnreadNotificationCount(
      String accountIdHeader, String accountRole) {
    UUID recipientAccountId = requireNotificationRecipient(accountIdHeader, accountRole);
    long unreadCount =
        notificationRepository.countByRecipientAccountIdAndRead(recipientAccountId, false);
    return new UnreadNotificationCountResponse(unreadCount);
  }

  public NotificationResponse markNotificationAsRead(
      UUID notificationId, String accountIdHeader, String accountRole) {
    return updateNotificationReadState(notificationId, accountIdHeader, accountRole, true);
  }

  public NotificationResponse markNotificationAsUnread(
      UUID notificationId, String accountIdHeader, String accountRole) {
    return updateNotificationReadState(notificationId, accountIdHeader, accountRole, false);
  }

  public UnreadNotificationCountResponse markAllNotificationsAsRead(
      String accountIdHeader, String accountRole) {
    UUID recipientAccountId = requireNotificationRecipient(accountIdHeader, accountRole);
    List<Notification> unreadNotifications =
        notificationRepository.findAllByRecipientAccountIdAndReadFalse(recipientAccountId);

    unreadNotifications.forEach(notification -> notification.setRead(true));
    if (!unreadNotifications.isEmpty()) {
      notificationRepository.saveAll(unreadNotifications);
    }

    return new UnreadNotificationCountResponse(0L);
  }

  public NotificationResponse createNotification(
      UUID sourceEventId,
      UUID recipientAccountId,
      NotificationType type,
      String title,
      String message,
      UUID referenceId) {

    if (notificationRepository.existsBySourceEventId(sourceEventId)) {
      return null;
    }

    Notification notification =
        Notification.builder()
            .id(UUID.randomUUID())
            .sourceEventId(sourceEventId)
            .recipientAccountId(recipientAccountId)
            .type(type)
            .title(title)
            .message(message)
            .referenceId(referenceId)
            .read(false)
            .createdAt(Instant.now())
            .build();

    Notification savedNotification = notificationRepository.save(notification);
    NotificationResponse response = notificationMapper.toNotificationResponse(savedNotification);

    notificationSseService.publishNotificationCreated(
        savedNotification.getRecipientAccountId(),
        savedNotification.getId(),
        savedNotification.getType(),
        savedNotification.getReferenceId());

    return response;
  }

  private NotificationResponse updateNotificationReadState(
      UUID notificationId, String accountIdHeader, String accountRole, boolean readState) {
    UUID recipientAccountId = requireNotificationRecipient(accountIdHeader, accountRole);

    Notification notification =
        notificationRepository
            .findByIdAndRecipientAccountId(notificationId, recipientAccountId)
            .orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_NOT_FOUND));

    if (notification.isRead() != readState) {
      notification.setRead(readState);
      notification = notificationRepository.save(notification);
    }

    return notificationMapper.toNotificationResponse(notification);
  }

  private UUID requireNotificationRecipient(String accountIdHeader, String accountRole) {

    if (accountIdHeader == null || accountIdHeader.isBlank()) {
      throw new AppException(ErrorCode.UNAUTHENTICATED);
    }

    boolean supportedRole = "CANDIDATE".equals(accountRole) || "RECRUITER".equals(accountRole);

    if (!supportedRole) {
      throw new AppException(ErrorCode.FORBIDDEN);
    }

    try {
      return UUID.fromString(accountIdHeader);
    } catch (IllegalArgumentException exception) {
      throw new AppException(ErrorCode.UNAUTHENTICATED);
    }
  }
}
