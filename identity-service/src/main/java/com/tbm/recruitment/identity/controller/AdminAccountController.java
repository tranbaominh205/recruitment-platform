package com.tbm.recruitment.identity.controller;

import com.tbm.recruitment.identity.dto.request.UpdateAccountEnabledRequest;
import com.tbm.recruitment.identity.dto.response.AccountResponse;
import com.tbm.recruitment.identity.dto.response.AdminAccountStatisticsResponse;
import com.tbm.recruitment.identity.dto.response.ApiResponse;
import com.tbm.recruitment.identity.dto.response.PageResponse;
import com.tbm.recruitment.identity.exception.ErrorCode;
import com.tbm.recruitment.identity.service.AccountService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/identity/admin")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminAccountController {

  AccountService accountService;

  @GetMapping("/accounts")
  public ApiResponse<PageResponse<AccountResponse>> getAccounts(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String role,
      @RequestParam(required = false) Boolean enabled,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return ApiResponse.<PageResponse<AccountResponse>>builder()
        .code(ErrorCode.SUCCESS.getCode())
        .message(ErrorCode.SUCCESS.getMessage())
        .result(accountService.getAccounts(keyword, role, enabled, page, size))
        .build();
  }

  @GetMapping("/accounts/{accountId}")
  public ApiResponse<AccountResponse> getAccountById(@PathVariable UUID accountId) {
    return ApiResponse.<AccountResponse>builder()
        .code(ErrorCode.SUCCESS.getCode())
        .message(ErrorCode.SUCCESS.getMessage())
        .result(accountService.getAccountById(accountId))
        .build();
  }

  @PatchMapping("/accounts/{accountId}/enabled")
  public ApiResponse<AccountResponse> updateAccountEnabled(
      @PathVariable UUID accountId,
      @Valid @RequestBody UpdateAccountEnabledRequest request,
      @AuthenticationPrincipal Jwt jwt) {
    return ApiResponse.<AccountResponse>builder()
        .code(ErrorCode.SUCCESS.getCode())
        .message(ErrorCode.SUCCESS.getMessage())
        .result(accountService.updateAccountEnabled(accountId, request.enabled(), jwt))
        .build();
  }

  @PostMapping("/accounts/{accountId}/revoke-sessions")
  public ApiResponse<AccountResponse> revokeSessions(
      @PathVariable UUID accountId, @AuthenticationPrincipal Jwt jwt) {
    return ApiResponse.<AccountResponse>builder()
        .code(ErrorCode.SUCCESS.getCode())
        .message(ErrorCode.SUCCESS.getMessage())
        .result(accountService.revokeSessions(accountId, jwt))
        .build();
  }

  @GetMapping("/statistics")
  public ApiResponse<AdminAccountStatisticsResponse> getAccountStatistics() {
    return ApiResponse.<AdminAccountStatisticsResponse>builder()
        .code(ErrorCode.SUCCESS.getCode())
        .message(ErrorCode.SUCCESS.getMessage())
        .result(accountService.getAdminStatistics())
        .build();
  }
}
