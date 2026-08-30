package com.tbm.recruitment.identity.controller;

import com.tbm.recruitment.identity.dto.request.RegisterRequest;
import com.tbm.recruitment.identity.dto.response.AccountResponse;
import com.tbm.recruitment.identity.dto.response.ApiResponse;
import com.tbm.recruitment.identity.exception.ErrorCode;
import com.tbm.recruitment.identity.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/identity/auth")
@RequiredArgsConstructor
public class AuthenticationController {

  private final AuthenticationService authenticationService;

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<AccountResponse> register(@Valid @RequestBody RegisterRequest request) {

    return ApiResponse.<AccountResponse>builder()
        .code(ErrorCode.SUCCESS.getCode())
        .message(ErrorCode.SUCCESS.getMessage())
        .result(authenticationService.register(request))
        .build();
  }
}
