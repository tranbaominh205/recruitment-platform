package com.tbm.recruitment.resume.controller;

import com.tbm.recruitment.resume.dto.response.AdminResumeResponse;
import com.tbm.recruitment.resume.dto.response.AdminResumeStatisticsResponse;
import com.tbm.recruitment.resume.dto.response.ApiResponse;
import com.tbm.recruitment.resume.dto.response.PageResponse;
import com.tbm.recruitment.resume.exception.ErrorCode;
import com.tbm.recruitment.resume.service.ResumeService;
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
@RequestMapping("/resume/admin")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminResumeController {

  ResumeService resumeService;

  @GetMapping("/resumes")
  public ApiResponse<PageResponse<AdminResumeResponse>> getResumes(
      @RequestHeader(value = "X-Account-Id", required = false) String accountId,
      @RequestHeader(value = "X-Account-Role", required = false) String accountRole,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String ownerAccountId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return ApiResponse.<PageResponse<AdminResumeResponse>>builder()
        .code(ErrorCode.SUCCESS.getCode())
        .message(ErrorCode.SUCCESS.getMessage())
        .result(
            resumeService.getAdminResumes(
                accountId, accountRole, status, ownerAccountId, page, size))
        .build();
  }

  @GetMapping("/resumes/{resumeId}")
  public ApiResponse<AdminResumeResponse> getResumeById(
      @PathVariable UUID resumeId,
      @RequestHeader(value = "X-Account-Id", required = false) String accountId,
      @RequestHeader(value = "X-Account-Role", required = false) String accountRole) {
    return ApiResponse.<AdminResumeResponse>builder()
        .code(ErrorCode.SUCCESS.getCode())
        .message(ErrorCode.SUCCESS.getMessage())
        .result(resumeService.getAdminResumeById(resumeId, accountId, accountRole))
        .build();
  }

  @GetMapping("/statistics")
  public ApiResponse<AdminResumeStatisticsResponse> getStatistics(
      @RequestHeader(value = "X-Account-Id", required = false) String accountId,
      @RequestHeader(value = "X-Account-Role", required = false) String accountRole) {
    return ApiResponse.<AdminResumeStatisticsResponse>builder()
        .code(ErrorCode.SUCCESS.getCode())
        .message(ErrorCode.SUCCESS.getMessage())
        .result(resumeService.getAdminStatistics(accountId, accountRole))
        .build();
  }
}
