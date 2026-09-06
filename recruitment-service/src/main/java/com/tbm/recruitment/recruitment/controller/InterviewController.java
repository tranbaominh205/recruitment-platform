package com.tbm.recruitment.recruitment.controller;

import com.tbm.recruitment.recruitment.dto.request.ScheduleInterviewRequest;
import com.tbm.recruitment.recruitment.dto.response.ApiResponse;
import com.tbm.recruitment.recruitment.dto.response.InterviewResponse;
import com.tbm.recruitment.recruitment.exception.ErrorCode;
import com.tbm.recruitment.recruitment.service.InterviewService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/recruitment/application/{applicationId}/interview")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InterviewController {

  InterviewService interviewService;

  @PostMapping
  public ResponseEntity<ApiResponse<InterviewResponse>> scheduleInterview(
      @PathVariable UUID applicationId,
      @RequestHeader(value = "X-Account-Id", required = false) String accountId,
      @RequestHeader(value = "X-Account-Role", required = false) String accountRole,
      @Valid @RequestBody ScheduleInterviewRequest request) {

    InterviewResponse result =
        interviewService.scheduleInterview(applicationId, accountId, accountRole, request);

    ApiResponse<InterviewResponse> response =
        ApiResponse.<InterviewResponse>builder()
            .code(ErrorCode.SUCCESS.getCode())
            .message(ErrorCode.SUCCESS.getMessage())
            .result(result)
            .build();

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping
  public ApiResponse<InterviewResponse> getInterview(
      @PathVariable UUID applicationId,
      @RequestHeader(value = "X-Account-Id", required = false) String accountId,
      @RequestHeader(value = "X-Account-Role", required = false) String accountRole) {

    InterviewResponse result = interviewService.getInterview(applicationId, accountId, accountRole);

    return ApiResponse.<InterviewResponse>builder()
        .code(ErrorCode.SUCCESS.getCode())
        .message(ErrorCode.SUCCESS.getMessage())
        .result(result)
        .build();
  }
}
