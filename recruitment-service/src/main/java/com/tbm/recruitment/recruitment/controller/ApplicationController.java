package com.tbm.recruitment.recruitment.controller;

import com.tbm.recruitment.recruitment.dto.request.CreateApplicationRequest;
import com.tbm.recruitment.recruitment.dto.request.UpdateApplicationStatusRequest;
import com.tbm.recruitment.recruitment.dto.response.ApiResponse;
import com.tbm.recruitment.recruitment.dto.response.ApplicationResponse;
import com.tbm.recruitment.recruitment.dto.response.PageResponse;
import com.tbm.recruitment.recruitment.exception.ErrorCode;
import com.tbm.recruitment.recruitment.service.ApplicationService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/recruitment/application")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ApplicationController {

  ApplicationService applicationService;

  @PostMapping
  public ResponseEntity<ApiResponse<ApplicationResponse>> submitApplication(
      @RequestHeader(value = "X-Account-Id", required = false) String accountId,
      @RequestHeader(value = "X-Account-Role", required = false) String accountRole,
      @Valid @RequestBody CreateApplicationRequest request) {

    ApplicationResponse result =
        applicationService.submitApplication(accountId, accountRole, request);

    ApiResponse<ApplicationResponse> response =
        ApiResponse.<ApplicationResponse>builder()
            .code(ErrorCode.SUCCESS.getCode())
            .message(ErrorCode.SUCCESS.getMessage())
            .result(result)
            .build();

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping("/mine")
  public ApiResponse<PageResponse<ApplicationResponse>> getMyApplications(
      @RequestHeader(value = "X-Account-Id", required = false) String accountId,
      @RequestHeader(value = "X-Account-Role", required = false) String accountRole,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {

    PageResponse<ApplicationResponse> result =
        applicationService.getMyApplications(accountId, accountRole, page, size);

    return ApiResponse.<PageResponse<ApplicationResponse>>builder()
        .code(ErrorCode.SUCCESS.getCode())
        .message(ErrorCode.SUCCESS.getMessage())
        .result(result)
        .build();
  }

  @GetMapping("/job/{jobId}")
  public ApiResponse<PageResponse<ApplicationResponse>> getApplicationsForJob(
      @PathVariable UUID jobId,
      @RequestHeader(value = "X-Account-Id", required = false) String accountId,
      @RequestHeader(value = "X-Account-Role", required = false) String accountRole,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {

    PageResponse<ApplicationResponse> result =
        applicationService.getApplicationsForOwnedJob(jobId, accountId, accountRole, page, size);

    return ApiResponse.<PageResponse<ApplicationResponse>>builder()
        .code(ErrorCode.SUCCESS.getCode())
        .message(ErrorCode.SUCCESS.getMessage())
        .result(result)
        .build();
  }

  @PatchMapping("/{applicationId}/status")
  public ApiResponse<ApplicationResponse> updateApplicationStatus(
      @PathVariable UUID applicationId,
      @RequestHeader(value = "X-Account-Id", required = false) String accountId,
      @RequestHeader(value = "X-Account-Role", required = false) String accountRole,
      @Valid @RequestBody UpdateApplicationStatusRequest request) {

    ApplicationResponse result =
        applicationService.updateApplicationStatus(applicationId, accountId, accountRole, request);

    return ApiResponse.<ApplicationResponse>builder()
        .code(ErrorCode.SUCCESS.getCode())
        .message(ErrorCode.SUCCESS.getMessage())
        .result(result)
        .build();
  }

  @PatchMapping("/{applicationId}/withdraw")
  public ApiResponse<ApplicationResponse> withdrawApplication(
      @PathVariable UUID applicationId,
      @RequestHeader(value = "X-Account-Id", required = false) String accountId,
      @RequestHeader(value = "X-Account-Role", required = false) String accountRole) {

    ApplicationResponse result =
        applicationService.withdrawApplication(applicationId, accountId, accountRole);

    return ApiResponse.<ApplicationResponse>builder()
        .code(ErrorCode.SUCCESS.getCode())
        .message(ErrorCode.SUCCESS.getMessage())
        .result(result)
        .build();
  }

  @GetMapping("/{applicationId}")
  public ApiResponse<ApplicationResponse> getApplication(
      @PathVariable UUID applicationId,
      @RequestHeader(value = "X-Account-Id", required = false) String accountId,
      @RequestHeader(value = "X-Account-Role", required = false) String accountRole) {

    ApplicationResponse result =
        applicationService.getApplication(applicationId, accountId, accountRole);

    return ApiResponse.<ApplicationResponse>builder()
        .code(ErrorCode.SUCCESS.getCode())
        .message(ErrorCode.SUCCESS.getMessage())
        .result(result)
        .build();
  }
}
