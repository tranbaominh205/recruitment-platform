package com.tbm.recruitment.matching.controller;

import com.tbm.recruitment.matching.dto.response.ApiResponse;
import com.tbm.recruitment.matching.dto.response.MatchResultResponse;
import com.tbm.recruitment.matching.service.ApplicationMatchingService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/matching/application")
@RequiredArgsConstructor
public class MatchingController {

  private final ApplicationMatchingService applicationMatchingService;

  @PostMapping("/{applicationId}")
  public ResponseEntity<ApiResponse<MatchResultResponse>> matchApplication(
      @PathVariable UUID applicationId,
      @RequestHeader("X-Account-Id") String accountId,
      @RequestHeader("X-Account-Role") String accountRole) {
    MatchResultResponse result =
        applicationMatchingService.matchApplication(applicationId, accountId, accountRole);
    return ResponseEntity.ok(ApiResponse.success(result));
  }

  @GetMapping("/{applicationId}")
  public ResponseEntity<ApiResponse<MatchResultResponse>> getMatchResult(
      @PathVariable UUID applicationId,
      @RequestHeader("X-Account-Id") String accountId,
      @RequestHeader("X-Account-Role") String accountRole) {
    MatchResultResponse result =
        applicationMatchingService.getMatchResult(applicationId, accountId, accountRole);
    return ResponseEntity.ok(ApiResponse.success(result));
  }
}
