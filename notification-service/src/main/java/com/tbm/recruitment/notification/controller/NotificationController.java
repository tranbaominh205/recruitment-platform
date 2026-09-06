package com.tbm.recruitment.notification.controller;

import com.tbm.recruitment.notification.dto.response.ApiResponse;
import com.tbm.recruitment.notification.dto.response.NotificationResponse;
import com.tbm.recruitment.notification.exception.ErrorCode;
import com.tbm.recruitment.notification.service.NotificationService;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationController {

  NotificationService notificationService;

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
}
