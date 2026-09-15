package com.tbm.recruitment.notification.controller;

import com.tbm.recruitment.notification.dto.response.ApiResponse;
import com.tbm.recruitment.notification.dto.response.NotificationResponse;
import com.tbm.recruitment.notification.dto.response.UnreadNotificationCountResponse;
import com.tbm.recruitment.notification.exception.ErrorCode;
import com.tbm.recruitment.notification.service.NotificationService;
import com.tbm.recruitment.notification.service.NotificationSseService;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationController {

  NotificationService notificationService;
  NotificationSseService notificationSseService;

  @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter streamEvents(
      @RequestHeader(value = "X-Account-Id", required = false) String accountId,
      @RequestHeader(value = "X-Account-Role", required = false) String accountRole) {
    return notificationSseService.subscribe(accountId, accountRole);
  }

  @GetMapping
  public ApiResponse<List<NotificationResponse>> getMyNotifications(
      @RequestHeader(value = "X-Account-Id", required = false) String accountId,
      @RequestHeader(value = "X-Account-Role", required = false) String accountRole) {

    List<NotificationResponse> result =
        notificationService.getMyNotifications(accountId, accountRole);

    return ApiResponse.<List<NotificationResponse>>builder()
        .code(ErrorCode.SUCCESS.getCode())
        .message(ErrorCode.SUCCESS.getMessage())
        .result(result)
        .build();
  }

  @GetMapping("/unread-count")
  public ApiResponse<UnreadNotificationCountResponse> getUnreadNotificationCount(
      @RequestHeader(value = "X-Account-Id", required = false) String accountId,
      @RequestHeader(value = "X-Account-Role", required = false) String accountRole) {
    return ApiResponse.<UnreadNotificationCountResponse>builder()
        .code(ErrorCode.SUCCESS.getCode())
        .message(ErrorCode.SUCCESS.getMessage())
        .result(notificationService.getUnreadNotificationCount(accountId, accountRole))
        .build();
  }

  @PatchMapping("/{notificationId}/read")
  public ApiResponse<NotificationResponse> markNotificationAsRead(
      @PathVariable UUID notificationId,
      @RequestHeader(value = "X-Account-Id", required = false) String accountId,
      @RequestHeader(value = "X-Account-Role", required = false) String accountRole) {
    return ApiResponse.<NotificationResponse>builder()
        .code(ErrorCode.SUCCESS.getCode())
        .message(ErrorCode.SUCCESS.getMessage())
        .result(notificationService.markNotificationAsRead(notificationId, accountId, accountRole))
        .build();
  }

  @PatchMapping("/{notificationId}/unread")
  public ApiResponse<NotificationResponse> markNotificationAsUnread(
      @PathVariable UUID notificationId,
      @RequestHeader(value = "X-Account-Id", required = false) String accountId,
      @RequestHeader(value = "X-Account-Role", required = false) String accountRole) {
    return ApiResponse.<NotificationResponse>builder()
        .code(ErrorCode.SUCCESS.getCode())
        .message(ErrorCode.SUCCESS.getMessage())
        .result(
            notificationService.markNotificationAsUnread(notificationId, accountId, accountRole))
        .build();
  }

  @PatchMapping("/read-all")
  public ApiResponse<UnreadNotificationCountResponse> markAllNotificationsAsRead(
      @RequestHeader(value = "X-Account-Id", required = false) String accountId,
      @RequestHeader(value = "X-Account-Role", required = false) String accountRole) {
    return ApiResponse.<UnreadNotificationCountResponse>builder()
        .code(ErrorCode.SUCCESS.getCode())
        .message(ErrorCode.SUCCESS.getMessage())
        .result(notificationService.markAllNotificationsAsRead(accountId, accountRole))
        .build();
  }
}
