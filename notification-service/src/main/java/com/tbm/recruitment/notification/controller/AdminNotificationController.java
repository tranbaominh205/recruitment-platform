package com.tbm.recruitment.notification.controller;

import com.tbm.recruitment.notification.dto.response.AdminNotificationStatisticsResponse;
import com.tbm.recruitment.notification.dto.response.ApiResponse;
import com.tbm.recruitment.notification.dto.response.NotificationResponse;
import com.tbm.recruitment.notification.dto.response.PageResponse;
import com.tbm.recruitment.notification.exception.ErrorCode;
import com.tbm.recruitment.notification.service.AdminNotificationService;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notification/admin")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminNotificationController {

  AdminNotificationService adminNotificationService;

  @GetMapping("/notifications")
  public ApiResponse<PageResponse<NotificationResponse>> getNotifications(
      @RequestHeader(value = "X-Account-Id", required = false) String accountId,
      @RequestHeader(value = "X-Account-Role", required = false) String accountRole,
      @RequestParam(required = false) String recipientAccountId,
      @RequestParam(required = false) String type,
      @RequestParam(required = false) Boolean read,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return ApiResponse.<PageResponse<NotificationResponse>>builder()
        .code(ErrorCode.SUCCESS.getCode())
        .message(ErrorCode.SUCCESS.getMessage())
        .result(
            adminNotificationService.getNotifications(
                accountId, accountRole, recipientAccountId, type, read, page, size))
        .build();
  }

  @GetMapping("/notifications/{notificationId}")
  public ApiResponse<NotificationResponse> getNotificationById(
      @PathVariable UUID notificationId,
      @RequestHeader(value = "X-Account-Id", required = false) String accountId,
      @RequestHeader(value = "X-Account-Role", required = false) String accountRole) {
    return ApiResponse.<NotificationResponse>builder()
        .code(ErrorCode.SUCCESS.getCode())
        .message(ErrorCode.SUCCESS.getMessage())
        .result(
            adminNotificationService.getNotificationById(notificationId, accountId, accountRole))
        .build();
  }

  @GetMapping("/statistics")
  public ApiResponse<AdminNotificationStatisticsResponse> getStatistics(
      @RequestHeader(value = "X-Account-Id", required = false) String accountId,
      @RequestHeader(value = "X-Account-Role", required = false) String accountRole) {
    return ApiResponse.<AdminNotificationStatisticsResponse>builder()
        .code(ErrorCode.SUCCESS.getCode())
        .message(ErrorCode.SUCCESS.getMessage())
        .result(adminNotificationService.getStatistics(accountId, accountRole))
        .build();
  }
}
