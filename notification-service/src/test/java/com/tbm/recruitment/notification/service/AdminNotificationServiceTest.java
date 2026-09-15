package com.tbm.recruitment.notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tbm.recruitment.notification.dto.response.AdminNotificationStatisticsResponse;
import com.tbm.recruitment.notification.dto.response.NotificationResponse;
import com.tbm.recruitment.notification.dto.response.PageResponse;
import com.tbm.recruitment.notification.entity.Notification;
import com.tbm.recruitment.notification.enums.NotificationType;
import com.tbm.recruitment.notification.exception.AppException;
import com.tbm.recruitment.notification.exception.ErrorCode;
import com.tbm.recruitment.notification.mapper.NotificationMapper;
import com.tbm.recruitment.notification.repository.NotificationRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;

@ExtendWith(MockitoExtension.class)
class AdminNotificationServiceTest {

  @Mock private NotificationRepository notificationRepository;
  @Mock private NotificationMapper notificationMapper;
  @Mock private MongoTemplate mongoTemplate;

  private AdminNotificationService service;

  @BeforeEach
  void setUp() {
    service =
        new AdminNotificationService(notificationRepository, notificationMapper, mongoTemplate);
  }

  @Test
  void adminCanListNotificationsWithFiltersAndPagination() {
    Notification notification =
        buildNotification(NotificationType.APPLICATION_STATUS_CHANGED, false);
    NotificationResponse mapped =
        new NotificationResponse(
            notification.getId(),
            notification.getType(),
            notification.getTitle(),
            notification.getMessage(),
            notification.getReferenceId(),
            notification.isRead(),
            notification.getCreatedAt());
    when(mongoTemplate.count(any(), org.mockito.ArgumentMatchers.eq(Notification.class)))
        .thenReturn(1L);
    when(mongoTemplate.find(any(), org.mockito.ArgumentMatchers.eq(Notification.class)))
        .thenReturn(List.of(notification));
    when(notificationMapper.toNotificationResponse(notification)).thenReturn(mapped);

    PageResponse<NotificationResponse> response =
        service.getNotifications(
            UUID.randomUUID().toString(),
            "ADMIN",
            notification.getRecipientAccountId().toString(),
            "APPLICATION_STATUS_CHANGED",
            false,
            0,
            20);

    assertEquals(1, response.content().size());
    assertEquals(notification.getId(), response.content().getFirst().id());
  }

  @Test
  void nonAdminRolesAreForbidden() {
    String accountId = UUID.randomUUID().toString();

    AppException exception =
        assertThrows(
            AppException.class,
            () -> service.getNotifications(accountId, "CANDIDATE", null, null, null, 0, 20));

    assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());

    AppException recruiterException =
        assertThrows(
            AppException.class,
            () -> service.getNotifications(accountId, "RECRUITER", null, null, null, 0, 20));

    assertEquals(ErrorCode.FORBIDDEN, recruiterException.getErrorCode());
  }

  @Test
  void statisticsAreCountedByTypeAndReadState() {
    when(notificationRepository.count()).thenReturn(30L);
    when(notificationRepository.countByRead(true)).thenReturn(18L);
    when(notificationRepository.countByRead(false)).thenReturn(12L);
    when(notificationRepository.countByType(NotificationType.APPLICATION_STATUS_CHANGED))
        .thenReturn(20L);
    when(notificationRepository.countByType(NotificationType.INTERVIEW_SCHEDULED)).thenReturn(10L);

    AdminNotificationStatisticsResponse response =
        service.getStatistics(UUID.randomUUID().toString(), "ADMIN");

    assertEquals(30L, response.totalNotifications());
    assertEquals(18L, response.readNotifications());
    assertEquals(12L, response.unreadNotifications());
    assertEquals(20L, response.applicationStatusChangedNotifications());
    assertEquals(10L, response.interviewScheduledNotifications());
    verify(notificationRepository).count();
  }

  private Notification buildNotification(NotificationType type, boolean read) {
    return Notification.builder()
        .id(UUID.randomUUID())
        .sourceEventId(UUID.randomUUID())
        .recipientAccountId(UUID.randomUUID())
        .type(type)
        .title("title")
        .message("message")
        .referenceId(UUID.randomUUID())
        .read(read)
        .createdAt(Instant.now())
        .build();
  }
}
