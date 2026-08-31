package com.tbm.recruitment.identity.controller;

import com.tbm.recruitment.identity.dto.response.AccountResponse;
import com.tbm.recruitment.identity.dto.response.ApiResponse;
import com.tbm.recruitment.identity.exception.ErrorCode;
import com.tbm.recruitment.identity.service.AccountService;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/identity/admin/accounts")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminAccountController {

  AccountService accountService;

  @GetMapping
  public ApiResponse<List<AccountResponse>> getAccounts() {
    return ApiResponse.<List<AccountResponse>>builder()
        .code(ErrorCode.SUCCESS.getCode())
        .message(ErrorCode.SUCCESS.getMessage())
        .result(accountService.getAccounts())
        .build();
  }
}
