package com.tbm.recruitment.candidate.controller;

import com.tbm.recruitment.candidate.dto.response.ApiResponse;
import com.tbm.recruitment.candidate.dto.response.CandidateAccountResponse;
import com.tbm.recruitment.candidate.exception.ErrorCode;
import com.tbm.recruitment.candidate.service.CandidateProfileService;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/candidate")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CandidateInternalController {

  CandidateProfileService candidateProfileService;

  @GetMapping("/{candidateId}/account")
  public ApiResponse<CandidateAccountResponse> getCandidateAccount(@PathVariable UUID candidateId) {

    CandidateAccountResponse result = candidateProfileService.getCandidateAccount(candidateId);

    return ApiResponse.<CandidateAccountResponse>builder()
        .code(ErrorCode.SUCCESS.getCode())
        .message(ErrorCode.SUCCESS.getMessage())
        .result(result)
        .build();
  }
}
