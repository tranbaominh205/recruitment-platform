package com.tbm.recruitment.candidate.controller;

import com.tbm.recruitment.candidate.dto.request.CreateCandidateProfileRequest;
import com.tbm.recruitment.candidate.dto.request.UpdateCandidatePreferencesRequest;
import com.tbm.recruitment.candidate.dto.request.UpdateCandidateProfileRequest;
import com.tbm.recruitment.candidate.dto.response.ApiResponse;
import com.tbm.recruitment.candidate.dto.response.CandidateProfileResponse;
import com.tbm.recruitment.candidate.exception.ErrorCode;
import com.tbm.recruitment.candidate.service.CandidateProfileService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/candidate/profile")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CandidateProfileController {

  CandidateProfileService candidateProfileService;

  @PostMapping
  public ResponseEntity<ApiResponse<CandidateProfileResponse>> createProfile(
      @RequestHeader(value = "X-Account-Id", required = false) String accountId,
      @RequestHeader(value = "X-Account-Role", required = false) String accountRole,
      @Valid @RequestBody CreateCandidateProfileRequest request) {

    CandidateProfileResponse result =
        candidateProfileService.createProfile(accountId, accountRole, request);

    ApiResponse<CandidateProfileResponse> response =
        ApiResponse.<CandidateProfileResponse>builder()
            .code(ErrorCode.SUCCESS.getCode())
            .message(ErrorCode.SUCCESS.getMessage())
            .result(result)
            .build();

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping
  public ApiResponse<CandidateProfileResponse> getMyProfile(
      @RequestHeader(value = "X-Account-Id", required = false) String accountId,
      @RequestHeader(value = "X-Account-Role", required = false) String accountRole) {

    CandidateProfileResponse result = candidateProfileService.getMyProfile(accountId, accountRole);

    return ApiResponse.<CandidateProfileResponse>builder()
        .code(ErrorCode.SUCCESS.getCode())
        .message(ErrorCode.SUCCESS.getMessage())
        .result(result)
        .build();
  }

  @PutMapping
  public ApiResponse<CandidateProfileResponse> updateMyProfile(
      @RequestHeader(value = "X-Account-Id", required = false) String accountId,
      @RequestHeader(value = "X-Account-Role", required = false) String accountRole,
      @Valid @RequestBody UpdateCandidateProfileRequest request) {

    CandidateProfileResponse result =
        candidateProfileService.updateMyProfile(accountId, accountRole, request);

    return ApiResponse.<CandidateProfileResponse>builder()
        .code(ErrorCode.SUCCESS.getCode())
        .message(ErrorCode.SUCCESS.getMessage())
        .result(result)
        .build();
  }

  @PutMapping("/preferences")
  public ApiResponse<CandidateProfileResponse> updateMyPreferences(
      @RequestHeader(value = "X-Account-Id", required = false) String accountId,
      @RequestHeader(value = "X-Account-Role", required = false) String accountRole,
      @Valid @RequestBody UpdateCandidatePreferencesRequest request) {

    CandidateProfileResponse result =
        candidateProfileService.updateMyPreferences(accountId, accountRole, request);

    return ApiResponse.<CandidateProfileResponse>builder()
        .code(ErrorCode.SUCCESS.getCode())
        .message(ErrorCode.SUCCESS.getMessage())
        .result(result)
        .build();
  }
}
