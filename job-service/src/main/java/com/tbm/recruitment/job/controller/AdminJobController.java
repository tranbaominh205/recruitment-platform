package com.tbm.recruitment.job.controller;

import com.tbm.recruitment.job.dto.response.AdminJobStatisticsResponse;
import com.tbm.recruitment.job.dto.response.ApiResponse;
import com.tbm.recruitment.job.dto.response.JobResponse;
import com.tbm.recruitment.job.dto.response.PageResponse;
import com.tbm.recruitment.job.exception.ErrorCode;
import com.tbm.recruitment.job.service.JobService;
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
@RequestMapping("/job/admin")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminJobController {

  JobService jobService;

  @GetMapping("/jobs")
  public ApiResponse<PageResponse<JobResponse>> getJobs(
      @RequestHeader(value = "X-Account-Id", required = false) String accountId,
      @RequestHeader(value = "X-Account-Role", required = false) String accountRole,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return ApiResponse.<PageResponse<JobResponse>>builder()
        .code(ErrorCode.SUCCESS.getCode())
        .message(ErrorCode.SUCCESS.getMessage())
        .result(jobService.getAdminJobs(accountId, accountRole, keyword, status, page, size))
        .build();
  }

  @GetMapping("/jobs/{jobId}")
  public ApiResponse<JobResponse> getJob(
      @PathVariable UUID jobId,
      @RequestHeader(value = "X-Account-Id", required = false) String accountId,
      @RequestHeader(value = "X-Account-Role", required = false) String accountRole) {
    return ApiResponse.<JobResponse>builder()
        .code(ErrorCode.SUCCESS.getCode())
        .message(ErrorCode.SUCCESS.getMessage())
        .result(jobService.getAdminJobById(jobId, accountId, accountRole))
        .build();
  }

  @GetMapping("/statistics")
  public ApiResponse<AdminJobStatisticsResponse> getStatistics(
      @RequestHeader(value = "X-Account-Id", required = false) String accountId,
      @RequestHeader(value = "X-Account-Role", required = false) String accountRole) {
    return ApiResponse.<AdminJobStatisticsResponse>builder()
        .code(ErrorCode.SUCCESS.getCode())
        .message(ErrorCode.SUCCESS.getMessage())
        .result(jobService.getAdminStatistics(accountId, accountRole))
        .build();
  }
}
