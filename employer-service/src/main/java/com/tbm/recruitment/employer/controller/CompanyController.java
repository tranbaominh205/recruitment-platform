package com.tbm.recruitment.employer.controller;

import com.tbm.recruitment.employer.dto.request.CreateCompanyRequest;
import com.tbm.recruitment.employer.dto.request.UpdateCompanyRequest;
import com.tbm.recruitment.employer.dto.response.ApiResponse;
import com.tbm.recruitment.employer.dto.response.CompanyResponse;
import com.tbm.recruitment.employer.exception.ErrorCode;
import com.tbm.recruitment.employer.service.CompanyService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/employer/company")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CompanyController {

  CompanyService companyService;

  @PostMapping
  public ResponseEntity<ApiResponse<CompanyResponse>> createCompany(
      @RequestHeader(value = "X-Account-Id", required = false) String accountId,
      @RequestHeader(value = "X-Account-Role", required = false) String accountRole,
      @Valid @RequestBody CreateCompanyRequest request) {

    CompanyResponse result = companyService.createCompany(accountId, accountRole, request);

    ApiResponse<CompanyResponse> response =
        ApiResponse.<CompanyResponse>builder()
            .code(ErrorCode.SUCCESS.getCode())
            .message(ErrorCode.SUCCESS.getMessage())
            .result(result)
            .build();

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping
  public ApiResponse<CompanyResponse> getMyCompany(
      @RequestHeader(value = "X-Account-Id", required = false) String accountId,
      @RequestHeader(value = "X-Account-Role", required = false) String accountRole) {

    CompanyResponse result = companyService.getMyCompany(accountId, accountRole);

    return ApiResponse.<CompanyResponse>builder()
        .code(ErrorCode.SUCCESS.getCode())
        .message(ErrorCode.SUCCESS.getMessage())
        .result(result)
        .build();
  }

  @PutMapping
  public ApiResponse<CompanyResponse> updateMyCompany(
      @RequestHeader(value = "X-Account-Id", required = false) String accountId,
      @RequestHeader(value = "X-Account-Role", required = false) String accountRole,
      @Valid @RequestBody UpdateCompanyRequest request) {

    CompanyResponse result = companyService.updateMyCompany(accountId, accountRole, request);

    return ApiResponse.<CompanyResponse>builder()
        .code(ErrorCode.SUCCESS.getCode())
        .message(ErrorCode.SUCCESS.getMessage())
        .result(result)
        .build();
  }
}
