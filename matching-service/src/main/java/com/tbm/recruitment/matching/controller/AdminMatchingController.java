package com.tbm.recruitment.matching.controller;

import com.tbm.recruitment.matching.dto.response.AdminMatchingStatisticsResponse;
import com.tbm.recruitment.matching.dto.response.ApiResponse;
import com.tbm.recruitment.matching.dto.response.MatchResultResponse;
import com.tbm.recruitment.matching.dto.response.PageResponse;
import com.tbm.recruitment.matching.service.AdminMatchingService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/matching/admin")
@RequiredArgsConstructor
public class AdminMatchingController {

  private final AdminMatchingService adminMatchingService;

  @GetMapping("/results")
  public ResponseEntity<ApiResponse<PageResponse<MatchResultResponse>>> getResults(
      @RequestHeader(value = "X-Account-Id", required = false) String accountId,
      @RequestHeader(value = "X-Account-Role", required = false) String accountRole,
      @RequestParam(required = false) UUID jobId,
      @RequestParam(required = false) UUID candidateId,
      @RequestParam(required = false) UUID resumeId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    PageResponse<MatchResultResponse> result =
        adminMatchingService.getResults(
            accountId, accountRole, jobId, candidateId, resumeId, page, size);
    return ResponseEntity.ok(ApiResponse.success(result));
  }

  @GetMapping("/results/{applicationId}")
  public ResponseEntity<ApiResponse<MatchResultResponse>> getResultByApplicationId(
      @PathVariable UUID applicationId,
      @RequestHeader(value = "X-Account-Id", required = false) String accountId,
      @RequestHeader(value = "X-Account-Role", required = false) String accountRole) {
    MatchResultResponse result =
        adminMatchingService.getResultByApplicationId(applicationId, accountId, accountRole);
    return ResponseEntity.ok(ApiResponse.success(result));
  }

  @GetMapping("/statistics")
  public ResponseEntity<ApiResponse<AdminMatchingStatisticsResponse>> getStatistics(
      @RequestHeader(value = "X-Account-Id", required = false) String accountId,
      @RequestHeader(value = "X-Account-Role", required = false) String accountRole) {
    AdminMatchingStatisticsResponse result =
        adminMatchingService.getStatistics(accountId, accountRole);
    return ResponseEntity.ok(ApiResponse.success(result));
  }
}
