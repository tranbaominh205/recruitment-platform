package com.tbm.recruitment.identity.controller;

import com.tbm.recruitment.identity.dto.request.ChangePasswordRequest;
import com.tbm.recruitment.identity.dto.response.ApiResponse;
import com.tbm.recruitment.identity.dto.response.MeResponse;
import com.tbm.recruitment.identity.exception.ErrorCode;
import com.tbm.recruitment.identity.service.AccountService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/identity")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AccountController {

  AccountService accountService;

  @GetMapping("/me")
  public ApiResponse<MeResponse> me(@AuthenticationPrincipal Jwt jwt) {

    return ApiResponse.<MeResponse>builder()
        .code(ErrorCode.SUCCESS.getCode())
        .message(ErrorCode.SUCCESS.getMessage())
        .result(accountService.getCurrentAccount(jwt))
        .build();
  }

  @PutMapping("/me/password")
  public ApiResponse<Void> changePassword(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ChangePasswordRequest request) {
    accountService.changePassword(jwt, request);
    return ApiResponse.<Void>builder()
        .code(ErrorCode.SUCCESS.getCode())
        .message(ErrorCode.SUCCESS.getMessage())
        .build();
  }
}
