package com.tbm.recruitment.identity.controller;

import com.tbm.recruitment.identity.dto.request.IntrospectRequest;
import com.tbm.recruitment.identity.dto.request.LoginRequest;
import com.tbm.recruitment.identity.dto.request.RegisterRequest;
import com.tbm.recruitment.identity.dto.response.AccountResponse;
import com.tbm.recruitment.identity.dto.response.ApiResponse;
import com.tbm.recruitment.identity.dto.response.IntrospectResponse;
import com.tbm.recruitment.identity.dto.response.LoginResponse;
import com.tbm.recruitment.identity.exception.ErrorCode;
import com.tbm.recruitment.identity.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/identity/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationController {

  AuthenticationService authenticationService;

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<AccountResponse> register(@Valid @RequestBody RegisterRequest request) {

    return ApiResponse.<AccountResponse>builder()
        .code(ErrorCode.SUCCESS.getCode())
        .message(ErrorCode.SUCCESS.getMessage())
        .result(authenticationService.register(request))
        .build();
  }

  @PostMapping("/login")
  public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {

    return ApiResponse.<LoginResponse>builder()
        .code(ErrorCode.SUCCESS.getCode())
        .message(ErrorCode.SUCCESS.getMessage())
        .result(authenticationService.login(request))
        .build();
  }

  @PostMapping("/introspect")
  public ApiResponse<IntrospectResponse> introspect(@Valid @RequestBody IntrospectRequest request) {

    return ApiResponse.<IntrospectResponse>builder()
        .code(ErrorCode.SUCCESS.getCode())
        .message(ErrorCode.SUCCESS.getMessage())
        .result(authenticationService.introspect(request))
        .build();
  }
}
