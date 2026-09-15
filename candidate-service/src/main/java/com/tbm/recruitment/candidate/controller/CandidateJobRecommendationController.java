package com.tbm.recruitment.candidate.controller;

import com.tbm.recruitment.candidate.dto.response.ApiResponse;
import com.tbm.recruitment.candidate.dto.response.PageResponse;
import com.tbm.recruitment.candidate.dto.response.RecommendedJobResponse;
import com.tbm.recruitment.candidate.exception.ErrorCode;
import com.tbm.recruitment.candidate.service.CandidateJobRecommendationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/candidate/recommendations")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CandidateJobRecommendationController {

  CandidateJobRecommendationService candidateJobRecommendationService;

  @GetMapping
  public ApiResponse<PageResponse<RecommendedJobResponse>> getRecommendations(
      @RequestHeader(value = "X-Account-Id", required = false) String accountId,
      @RequestHeader(value = "X-Account-Role", required = false) String accountRole,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {

    PageResponse<RecommendedJobResponse> result =
        candidateJobRecommendationService.getRecommendations(accountId, accountRole, page, size);

    return ApiResponse.<PageResponse<RecommendedJobResponse>>builder()
        .code(ErrorCode.SUCCESS.getCode())
        .message(ErrorCode.SUCCESS.getMessage())
        .result(result)
        .build();
  }
}
