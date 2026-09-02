package com.tbm.recruitment.job.controller;

import com.tbm.recruitment.job.dto.request.CreateJobRequest;
import com.tbm.recruitment.job.dto.request.UpdateJobRequest;
import com.tbm.recruitment.job.dto.response.ApiResponse;
import com.tbm.recruitment.job.dto.response.JobResponse;
import com.tbm.recruitment.job.dto.response.PageResponse;
import com.tbm.recruitment.job.exception.ErrorCode;
import com.tbm.recruitment.job.service.JobService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/job")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class JobController {

  JobService jobService;

  @PostMapping
  public ResponseEntity<ApiResponse<JobResponse>> createJob(
      @RequestHeader(value = "X-Account-Id", required = false) String accountId,
      @RequestHeader(value = "X-Account-Role", required = false) String accountRole,
      @Valid @RequestBody CreateJobRequest request) {

    JobResponse result = jobService.createJob(accountId, accountRole, request);

    ApiResponse<JobResponse> response =
        ApiResponse.<JobResponse>builder()
            .code(ErrorCode.SUCCESS.getCode())
            .message(ErrorCode.SUCCESS.getMessage())
            .result(result)
            .build();

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @PutMapping("/{jobId}")
  public ApiResponse<JobResponse> updateJob(
      @PathVariable UUID jobId,
      @RequestHeader(value = "X-Account-Id", required = false) String accountId,
      @RequestHeader(value = "X-Account-Role", required = false) String accountRole,
      @Valid @RequestBody UpdateJobRequest request) {

    JobResponse result = jobService.updateJob(jobId, accountId, accountRole, request);

    return ApiResponse.<JobResponse>builder()
        .code(ErrorCode.SUCCESS.getCode())
        .message(ErrorCode.SUCCESS.getMessage())
        .result(result)
        .build();
  }

  @PostMapping("/{jobId}/publish")
  public ApiResponse<JobResponse> publishJob(
      @PathVariable UUID jobId,
      @RequestHeader(value = "X-Account-Id", required = false) String accountId,
      @RequestHeader(value = "X-Account-Role", required = false) String accountRole) {

    JobResponse result = jobService.publishJob(jobId, accountId, accountRole);

    return ApiResponse.<JobResponse>builder()
        .code(ErrorCode.SUCCESS.getCode())
        .message(ErrorCode.SUCCESS.getMessage())
        .result(result)
        .build();
  }

  @PostMapping("/{jobId}/close")
  public ApiResponse<JobResponse> closeJob(
      @PathVariable UUID jobId,
      @RequestHeader(value = "X-Account-Id", required = false) String accountId,
      @RequestHeader(value = "X-Account-Role", required = false) String accountRole) {

    JobResponse result = jobService.closeJob(jobId, accountId, accountRole);

    return ApiResponse.<JobResponse>builder()
        .code(ErrorCode.SUCCESS.getCode())
        .message(ErrorCode.SUCCESS.getMessage())
        .result(result)
        .build();
  }

  @GetMapping("/mine")
  public ApiResponse<PageResponse<JobResponse>> getMyJobs(
      @RequestHeader(value = "X-Account-Id", required = false) String accountId,
      @RequestHeader(value = "X-Account-Role", required = false) String accountRole,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {

    PageResponse<JobResponse> result = jobService.getMyJobs(accountId, accountRole, page, size);

    return ApiResponse.<PageResponse<JobResponse>>builder()
        .code(ErrorCode.SUCCESS.getCode())
        .message(ErrorCode.SUCCESS.getMessage())
        .result(result)
        .build();
  }

  @GetMapping("/search")
  public ApiResponse<PageResponse<JobResponse>> searchJobs(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String location,
      @RequestParam(required = false) String employmentType,
      @RequestParam(required = false) String workplaceType,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {

    PageResponse<JobResponse> result =
        jobService.searchPublishedJobs(
            keyword, location, employmentType, workplaceType, page, size);

    return ApiResponse.<PageResponse<JobResponse>>builder()
        .code(ErrorCode.SUCCESS.getCode())
        .message(ErrorCode.SUCCESS.getMessage())
        .result(result)
        .build();
  }
}
