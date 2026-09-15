package com.tbm.recruitment.recruitment.controller;

import com.tbm.recruitment.recruitment.dto.response.AdminRecruitmentStatisticsResponse;
import com.tbm.recruitment.recruitment.dto.response.ApiResponse;
import com.tbm.recruitment.recruitment.dto.response.ApplicationResponse;
import com.tbm.recruitment.recruitment.dto.response.InterviewResponse;
import com.tbm.recruitment.recruitment.dto.response.PageResponse;
import com.tbm.recruitment.recruitment.exception.ErrorCode;
import com.tbm.recruitment.recruitment.service.AdminRecruitmentService;
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
@RequestMapping("/recruitment/admin")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminRecruitmentController {

  AdminRecruitmentService adminRecruitmentService;

  @GetMapping("/applications")
  public ApiResponse<PageResponse<ApplicationResponse>> getApplications(
      @RequestHeader(value = "X-Account-Id", required = false) String accountId,
      @RequestHeader(value = "X-Account-Role", required = false) String accountRole,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) UUID jobId,
      @RequestParam(required = false) UUID candidateId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return ApiResponse.<PageResponse<ApplicationResponse>>builder()
        .code(ErrorCode.SUCCESS.getCode())
        .message(ErrorCode.SUCCESS.getMessage())
        .result(
            adminRecruitmentService.getApplications(
                accountId, accountRole, status, jobId, candidateId, page, size))
        .build();
  }

  @GetMapping("/applications/{applicationId}")
  public ApiResponse<ApplicationResponse> getApplicationById(
      @PathVariable UUID applicationId,
      @RequestHeader(value = "X-Account-Id", required = false) String accountId,
      @RequestHeader(value = "X-Account-Role", required = false) String accountRole) {
    return ApiResponse.<ApplicationResponse>builder()
        .code(ErrorCode.SUCCESS.getCode())
        .message(ErrorCode.SUCCESS.getMessage())
        .result(adminRecruitmentService.getApplicationById(applicationId, accountId, accountRole))
        .build();
  }

  @GetMapping("/interviews")
  public ApiResponse<PageResponse<InterviewResponse>> getInterviews(
      @RequestHeader(value = "X-Account-Id", required = false) String accountId,
      @RequestHeader(value = "X-Account-Role", required = false) String accountRole,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return ApiResponse.<PageResponse<InterviewResponse>>builder()
        .code(ErrorCode.SUCCESS.getCode())
        .message(ErrorCode.SUCCESS.getMessage())
        .result(adminRecruitmentService.getInterviews(accountId, accountRole, page, size))
        .build();
  }

  @GetMapping("/interviews/{interviewId}")
  public ApiResponse<InterviewResponse> getInterviewById(
      @PathVariable UUID interviewId,
      @RequestHeader(value = "X-Account-Id", required = false) String accountId,
      @RequestHeader(value = "X-Account-Role", required = false) String accountRole) {
    return ApiResponse.<InterviewResponse>builder()
        .code(ErrorCode.SUCCESS.getCode())
        .message(ErrorCode.SUCCESS.getMessage())
        .result(adminRecruitmentService.getInterviewById(interviewId, accountId, accountRole))
        .build();
  }

  @GetMapping("/statistics")
  public ApiResponse<AdminRecruitmentStatisticsResponse> getStatistics(
      @RequestHeader(value = "X-Account-Id", required = false) String accountId,
      @RequestHeader(value = "X-Account-Role", required = false) String accountRole) {
    return ApiResponse.<AdminRecruitmentStatisticsResponse>builder()
        .code(ErrorCode.SUCCESS.getCode())
        .message(ErrorCode.SUCCESS.getMessage())
        .result(adminRecruitmentService.getStatistics(accountId, accountRole))
        .build();
  }
}
