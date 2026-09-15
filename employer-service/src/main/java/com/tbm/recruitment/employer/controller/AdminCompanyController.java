package com.tbm.recruitment.employer.controller;

import com.tbm.recruitment.employer.dto.request.UpdateCompanyModerationRequest;
import com.tbm.recruitment.employer.dto.request.UpdateCompanyVerificationRequest;
import com.tbm.recruitment.employer.dto.response.AdminCompanyResponse;
import com.tbm.recruitment.employer.dto.response.AdminCompanyStatisticsResponse;
import com.tbm.recruitment.employer.dto.response.ApiResponse;
import com.tbm.recruitment.employer.dto.response.PageResponse;
import com.tbm.recruitment.employer.exception.ErrorCode;
import com.tbm.recruitment.employer.service.CompanyService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/employer/admin")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminCompanyController {

  CompanyService companyService;

  @GetMapping("/companies")
  public ApiResponse<PageResponse<AdminCompanyResponse>> getCompanies(
      @RequestHeader(value = "X-Account-Id", required = false) String accountId,
      @RequestHeader(value = "X-Account-Role", required = false) String accountRole,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String moderationStatus,
      @RequestParam(required = false) String verificationStatus,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return ApiResponse.<PageResponse<AdminCompanyResponse>>builder()
        .code(ErrorCode.SUCCESS.getCode())
        .message(ErrorCode.SUCCESS.getMessage())
        .result(
            companyService.getAdminCompanies(
                accountId, accountRole, keyword, moderationStatus, verificationStatus, page, size))
        .build();
  }

  @GetMapping("/companies/{companyId}")
  public ApiResponse<AdminCompanyResponse> getCompany(
      @PathVariable UUID companyId,
      @RequestHeader(value = "X-Account-Id", required = false) String accountId,
      @RequestHeader(value = "X-Account-Role", required = false) String accountRole) {
    return ApiResponse.<AdminCompanyResponse>builder()
        .code(ErrorCode.SUCCESS.getCode())
        .message(ErrorCode.SUCCESS.getMessage())
        .result(companyService.getAdminCompanyById(companyId, accountId, accountRole))
        .build();
  }

  @PatchMapping("/companies/{companyId}/moderation")
  public ApiResponse<AdminCompanyResponse> moderateCompany(
      @PathVariable UUID companyId,
      @RequestHeader(value = "X-Account-Id", required = false) String accountId,
      @RequestHeader(value = "X-Account-Role", required = false) String accountRole,
      @RequestHeader(value = "X-Account-Permissions", required = false) String accountPermissions,
      @Valid @RequestBody UpdateCompanyModerationRequest request) {
    return ApiResponse.<AdminCompanyResponse>builder()
        .code(ErrorCode.SUCCESS.getCode())
        .message(ErrorCode.SUCCESS.getMessage())
        .result(
            companyService.updateCompanyModeration(
                companyId, accountId, accountRole, accountPermissions, request))
        .build();
  }

  @PatchMapping("/companies/{companyId}/verification")
  public ApiResponse<AdminCompanyResponse> verifyCompany(
      @PathVariable UUID companyId,
      @RequestHeader(value = "X-Account-Id", required = false) String accountId,
      @RequestHeader(value = "X-Account-Role", required = false) String accountRole,
      @RequestHeader(value = "X-Account-Permissions", required = false) String accountPermissions,
      @Valid @RequestBody UpdateCompanyVerificationRequest request) {
    return ApiResponse.<AdminCompanyResponse>builder()
        .code(ErrorCode.SUCCESS.getCode())
        .message(ErrorCode.SUCCESS.getMessage())
        .result(
            companyService.updateCompanyVerification(
                companyId, accountId, accountRole, accountPermissions, request))
        .build();
  }

  @GetMapping("/statistics")
  public ApiResponse<AdminCompanyStatisticsResponse> getStatistics(
      @RequestHeader(value = "X-Account-Id", required = false) String accountId,
      @RequestHeader(value = "X-Account-Role", required = false) String accountRole) {
    return ApiResponse.<AdminCompanyStatisticsResponse>builder()
        .code(ErrorCode.SUCCESS.getCode())
        .message(ErrorCode.SUCCESS.getMessage())
        .result(companyService.getAdminStatistics(accountId, accountRole))
        .build();
  }
}
