package com.tbm.recruitment.recruitment.controller;

import com.tbm.recruitment.recruitment.dto.request.CreateApplicationRequest;
import com.tbm.recruitment.recruitment.dto.response.ApiResponse;
import com.tbm.recruitment.recruitment.dto.response.ApplicationResponse;
import com.tbm.recruitment.recruitment.exception.ErrorCode;
import com.tbm.recruitment.recruitment.service.ApplicationService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
