package com.tbm.recruitment.notification.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

  @Mock private NotificationRepository notificationRepository;
  @Mock private NotificationMapper notificationMapper;

  private NotificationService notificationService;

  @BeforeEach
  void setUp() {
    notificationService = new NotificationService(notificationRepository, notificationMapper);
  }

  @Test
  void unreadCountOnlyCountsOwnUnreadNotifications() {
    UUID accountId = UUID.randomUUID();
    when(notificationRepository.countByRecipientAccountIdAndRead(accountId, false)).thenReturn(2L);

    UnreadNotificationCountResponse result =
        notificationService.getUnreadNotificationCount(accountId.toString(), "CANDIDATE");

    assertEquals(2L, result.unreadCount());
    verify(notificationRepository).countByRecipientAccountIdAndRead(accountId, false);
  }

  @Test
  void markOwnNotificationRead() {
    UUID accountId = UUID.randomUUID();
    Notification notification = buildNotification(accountId, false);
    NotificationResponse mapped = toResponse(notification, true);
    when(notificationRepository.findByIdAndRecipientAccountId(notification.getId(), accountId))
        .thenReturn(Optional.of(notification));
    when(notificationRepository.save(notification)).thenReturn(notification);
    when(notificationMapper.toNotificationResponse(notification)).thenReturn(mapped);

    NotificationResponse result =
        notificationService.markNotificationAsRead(
            notification.getId(), accountId.toString(), "CANDIDATE");

    assertTrue(result.read());
    verify(notificationRepository).save(notification);
  }

  @Test
  void markReadIsIdempotent() {
    UUID accountId = UUID.randomUUID();
    Notification notification = buildNotification(accountId, true);
    NotificationResponse mapped = toResponse(notification, true);
    when(notificationRepository.findByIdAndRecipientAccountId(notification.getId(), accountId))
        .thenReturn(Optional.of(notification));
    when(notificationMapper.toNotificationResponse(notification)).thenReturn(mapped);

    NotificationResponse result =
        notificationService.markNotificationAsRead(
            notification.getId(), accountId.toString(), "CANDIDATE");

    assertTrue(result.read());
    verify(notificationRepository, never()).save(any(Notification.class));
  }

  @Test
  void markOwnNotificationUnread() {
    UUID accountId = UUID.randomUUID();
    Notification notification = buildNotification(accountId, true);
    NotificationResponse mapped = toResponse(notification, false);
    when(notificationRepository.findByIdAndRecipientAccountId(notification.getId(), accountId))
        .thenReturn(Optional.of(notification));
    when(notificationRepository.save(notification)).thenReturn(notification);
    when(notificationMapper.toNotificationResponse(notification)).thenReturn(mapped);

    NotificationResponse result =
        notificationService.markNotificationAsUnread(
            notification.getId(), accountId.toString(), "CANDIDATE");

    assertFalse(result.read());
    verify(notificationRepository).save(notification);
  }

  @Test
  void markUnreadIsIdempotent() {
    UUID accountId = UUID.randomUUID();
    Notification notification = buildNotification(accountId, false);
    NotificationResponse mapped = toResponse(notification, false);
    when(notificationRepository.findByIdAndRecipientAccountId(notification.getId(), accountId))
        .thenReturn(Optional.of(notification));
    when(notificationMapper.toNotificationResponse(notification)).thenReturn(mapped);

    NotificationResponse result =
        notificationService.markNotificationAsUnread(
            notification.getId(), accountId.toString(), "CANDIDATE");

    assertFalse(result.read());
    verify(notificationRepository, never()).save(any(Notification.class));
  }

  @Test
  void foreignOrNonexistentNotificationReturnsNotFound() {
    UUID accountId = UUID.randomUUID();
    UUID notificationId = UUID.randomUUID();
    when(notificationRepository.findByIdAndRecipientAccountId(notificationId, accountId))
        .thenReturn(Optional.empty());

    AppException exception =
        assertThrows(
            AppException.class,
            () ->
                notificationService.markNotificationAsRead(
                    notificationId, accountId.toString(), "CANDIDATE"));

    assertEquals(ErrorCode.NOTIFICATION_NOT_FOUND, exception.getErrorCode());
  }

  @Test
  void markAllReadsOnlyCurrentAccountsUnreadNotifications() {
    UUID accountId = UUID.randomUUID();
    Notification firstUnread = buildNotification(accountId, false);
    Notification secondUnread = buildNotification(accountId, false);
    List<Notification> unreadNotifications = List.of(firstUnread, secondUnread);
    when(notificationRepository.findAllByRecipientAccountIdAndReadFalse(accountId))
        .thenReturn(unreadNotifications);
    when(notificationRepository.saveAll(unreadNotifications)).thenReturn(unreadNotifications);

    UnreadNotificationCountResponse result =
        notificationService.markAllNotificationsAsRead(accountId.toString(), "CANDIDATE");

    assertEquals(0L, result.unreadCount());
    assertTrue(firstUnread.isRead());
    assertTrue(secondUnread.isRead());
    verify(notificationRepository).findAllByRecipientAccountIdAndReadFalse(accountId);
    verify(notificationRepository).saveAll(unreadNotifications);
  }

  @Test
  void markAllWhenNoUnreadNotificationsSucceeds() {
    UUID accountId = UUID.randomUUID();
    when(notificationRepository.findAllByRecipientAccountIdAndReadFalse(accountId))
        .thenReturn(List.of());

    UnreadNotificationCountResponse result =
        notificationService.markAllNotificationsAsRead(accountId.toString(), "CANDIDATE");

    assertEquals(0L, result.unreadCount());
    verify(notificationRepository).findAllByRecipientAccountIdAndReadFalse(accountId);
    verify(notificationRepository, never()).saveAll(any());
  }

  @Test
  void candidateRoleAccepted() {
    UUID accountId = UUID.randomUUID();
    when(notificationRepository.countByRecipientAccountIdAndRead(accountId, false)).thenReturn(0L);

    assertDoesNotThrow(
        () -> notificationService.getUnreadNotificationCount(accountId.toString(), "CANDIDATE"));
  }

  @Test
  void recruiterRoleAccepted() {
    UUID accountId = UUID.randomUUID();
    when(notificationRepository.countByRecipientAccountIdAndRead(accountId, false)).thenReturn(0L);

    assertDoesNotThrow(
        () -> notificationService.getUnreadNotificationCount(accountId.toString(), "RECRUITER"));
  }

  @Test
  void adminRoleRejectedForSelfServiceOperations() {
    AppException exception =
        assertThrows(
            AppException.class,
            () ->
                notificationService.getUnreadNotificationCount(
                    UUID.randomUUID().toString(), "ADMIN"));

    assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());
  }

  @Test
  void invalidOrMissingAccountIdRetainsAuthenticationBehavior() {
    AppException missingAccountIdException =
        assertThrows(
            AppException.class,
            () -> notificationService.getUnreadNotificationCount(null, "CANDIDATE"));
    assertEquals(ErrorCode.UNAUTHENTICATED, missingAccountIdException.getErrorCode());

    AppException invalidAccountIdException =
        assertThrows(
            AppException.class,
            () -> notificationService.getUnreadNotificationCount("not-a-uuid", "CANDIDATE"));
    assertEquals(ErrorCode.UNAUTHENTICATED, invalidAccountIdException.getErrorCode());
  }

  private Notification buildNotification(UUID accountId, boolean read) {
    return Notification.builder()
        .id(UUID.randomUUID())
        .sourceEventId(UUID.randomUUID())
        .recipientAccountId(accountId)
        .type(NotificationType.APPLICATION_STATUS_CHANGED)
        .title("title")
        .message("message")
        .referenceId(UUID.randomUUID())
        .read(read)
        .createdAt(Instant.now())
        .build();
  }

  private NotificationResponse toResponse(Notification notification, boolean read) {
    return new NotificationResponse(
        notification.getId(),
        notification.getType(),
        notification.getTitle(),
        notification.getMessage(),
        notification.getReferenceId(),
        read,
        notification.getCreatedAt());
  }
}
