package com.tbm.recruitment.identity.controller;

import com.tbm.recruitment.identity.dto.response.ApiResponse;
import com.tbm.recruitment.identity.dto.response.MeResponse;
import com.tbm.recruitment.identity.exception.ErrorCode;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/identity")
public class AccountController {

  @GetMapping("/me")
  public ApiResponse<MeResponse> me(@AuthenticationPrincipal Jwt jwt) {

    MeResponse result =
        new MeResponse(
            jwt.getSubject(), jwt.getClaimAsString("email"), jwt.getClaimAsString("role"));

    return ApiResponse.<MeResponse>builder()
        .code(ErrorCode.SUCCESS.getCode())
        .message(ErrorCode.SUCCESS.getMessage())
        .result(result)
        .build();
  }
}
