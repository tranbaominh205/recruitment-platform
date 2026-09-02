package com.tbm.recruitment.resume.controller;

import com.tbm.recruitment.resume.dto.response.ApiResponse;
import com.tbm.recruitment.resume.dto.response.ResumeResponse;
import com.tbm.recruitment.resume.exception.ErrorCode;
import com.tbm.recruitment.resume.service.ResumeService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/resume")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ResumeController {

  ResumeService resumeService;

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ApiResponse<ResumeResponse>> uploadResume(
      @RequestHeader(value = "X-Account-Id", required = false) String accountId,
      @RequestHeader(value = "X-Account-Role", required = false) String accountRole,
      @RequestPart("file") MultipartFile file) {

    ResumeResponse result = resumeService.uploadResume(accountId, accountRole, file);

    ApiResponse<ResumeResponse> response =
        ApiResponse.<ResumeResponse>builder()
            .code(ErrorCode.SUCCESS.getCode())
            .message(ErrorCode.SUCCESS.getMessage())
            .result(result)
            .build();

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }
}
