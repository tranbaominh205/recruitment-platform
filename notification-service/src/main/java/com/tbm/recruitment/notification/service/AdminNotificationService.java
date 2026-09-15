package com.tbm.recruitment.notification.service;

import com.tbm.recruitment.notification.dto.response.AdminNotificationStatisticsResponse;
import com.tbm.recruitment.notification.dto.response.NotificationResponse;
import com.tbm.recruitment.notification.dto.response.PageResponse;
import com.tbm.recruitment.notification.entity.Notification;
import com.tbm.recruitment.notification.enums.NotificationType;
import com.tbm.recruitment.notification.exception.AppException;
import com.tbm.recruitment.notification.exception.ErrorCode;
import com.tbm.recruitment.notification.mapper.NotificationMapper;
import com.tbm.recruitment.notification.repository.NotificationRepository;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminNotificationService {

  NotificationRepository notificationRepository;
  NotificationMapper notificationMapper;
  MongoTemplate mongoTemplate;

  public PageResponse<NotificationResponse> getNotifications(
      String accountIdHeader,
      String accountRole,
      String recipientAccountId,
      String type,
      Boolean read,
      int page,
      int size) {
    requireAdminAccount(accountIdHeader, accountRole);
    validatePagination(page, size);

    UUID recipientFilter = parseOptionalUuid(recipientAccountId);
    NotificationType typeFilter = parseOptionalType(type);

    Query query = new Query();
    if (recipientFilter != null) {
      query.addCriteria(Criteria.where("recipientAccountId").is(recipientFilter));
    }
    if (typeFilter != null) {
      query.addCriteria(Criteria.where("type").is(typeFilter));
    }
    if (read != null) {
      query.addCriteria(Criteria.where("read").is(read));
    }

    long totalElements = mongoTemplate.count(query, Notification.class);
    query.with(PageRequest.of(page, size)).with(Sort.by(Sort.Direction.DESC, "createdAt"));

    List<NotificationResponse> content =
        mongoTemplate.find(query, Notification.class).stream()
            .map(notificationMapper::toNotificationResponse)
            .toList();
    int totalPages = (int) Math.ceil((double) totalElements / size);

    return new PageResponse<>(content, page, size, totalElements, totalPages);
  }

  public NotificationResponse getNotificationById(
      UUID notificationId, String accountIdHeader, String accountRole) {
    requireAdminAccount(accountIdHeader, accountRole);
    Notification notification =
        notificationRepository
            .findById(notificationId)
            .orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_NOT_FOUND));
    return notificationMapper.toNotificationResponse(notification);
  }

  public AdminNotificationStatisticsResponse getStatistics(
      String accountIdHeader, String accountRole) {
    requireAdminAccount(accountIdHeader, accountRole);

    long total = notificationRepository.count();
    long read = notificationRepository.countByRead(true);
    long unread = notificationRepository.countByRead(false);
    long applicationStatusChanged =
        notificationRepository.countByType(NotificationType.APPLICATION_STATUS_CHANGED);
    long interviewScheduled =
        notificationRepository.countByType(NotificationType.INTERVIEW_SCHEDULED);

    return new AdminNotificationStatisticsResponse(
        total, read, unread, applicationStatusChanged, interviewScheduled);
  }

  private void requireAdminAccount(String accountIdHeader, String accountRole) {
    if (!StringUtils.hasText(accountIdHeader)) {
      throw new AppException(ErrorCode.UNAUTHENTICATED);
    }
    try {
      UUID.fromString(accountIdHeader.trim());
    } catch (IllegalArgumentException exception) {
      throw new AppException(ErrorCode.UNAUTHENTICATED);
    }
    if (!"ADMIN".equals(accountRole)) {
      throw new AppException(ErrorCode.FORBIDDEN);
    }
  }

  private UUID parseOptionalUuid(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    try {
      return UUID.fromString(value.trim());
    } catch (IllegalArgumentException exception) {
      throw new AppException(ErrorCode.INVALID_REQUEST);
    }
  }

  private NotificationType parseOptionalType(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    try {
      return NotificationType.valueOf(value.trim().toUpperCase());
    } catch (IllegalArgumentException exception) {
      throw new AppException(ErrorCode.INVALID_REQUEST);
    }
  }

  private void validatePagination(int page, int size) {
    if (page < 0 || size < 1 || size > 100) {
      throw new AppException(ErrorCode.INVALID_REQUEST);
    }
  }
}
