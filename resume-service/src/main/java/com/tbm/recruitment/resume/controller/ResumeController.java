package com.tbm.recruitment.resume.controller;

import com.tbm.recruitment.resume.dto.response.ApiResponse;
import com.tbm.recruitment.resume.dto.response.ResumeDownloadResponse;
import com.tbm.recruitment.resume.dto.response.ResumeResponse;
import com.tbm.recruitment.resume.exception.ErrorCode;
import com.tbm.recruitment.resume.service.ResumeService;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

  @GetMapping
  public ApiResponse<List<ResumeResponse>> getMyResumes(
      @RequestHeader(value = "X-Account-Id", required = false) String accountId,
      @RequestHeader(value = "X-Account-Role", required = false) String accountRole) {

    List<ResumeResponse> result = resumeService.getMyResumes(accountId, accountRole);

    return ApiResponse.<List<ResumeResponse>>builder()
        .code(ErrorCode.SUCCESS.getCode())
        .message(ErrorCode.SUCCESS.getMessage())
        .result(result)
        .build();
  }

  @GetMapping("/{resumeId}")
  public ApiResponse<ResumeResponse> getMyResume(
      @PathVariable UUID resumeId,
      @RequestHeader(value = "X-Account-Id", required = false) String accountId,
      @RequestHeader(value = "X-Account-Role", required = false) String accountRole) {

    ResumeResponse result = resumeService.getMyResume(resumeId, accountId, accountRole);

    return ApiResponse.<ResumeResponse>builder()
        .code(ErrorCode.SUCCESS.getCode())
        .message(ErrorCode.SUCCESS.getMessage())
        .result(result)
        .build();
  }

  @GetMapping("/{resumeId}/download")
  public ResponseEntity<byte[]> downloadMyResume(
      @PathVariable UUID resumeId,
      @RequestHeader(value = "X-Account-Id", required = false) String accountId,
      @RequestHeader(value = "X-Account-Role", required = false) String accountRole) {

    ResumeDownloadResponse result =
        resumeService.downloadMyResume(resumeId, accountId, accountRole);

    ContentDisposition contentDisposition =
        ContentDisposition.attachment().filename(result.fileName()).build();

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
        .contentType(resolveContentType(result.contentType()))
        .contentLength(result.content().length)
        .body(result.content());
  }

  private MediaType resolveContentType(String contentType) {

    if (contentType == null || contentType.isBlank()) {
      return MediaType.APPLICATION_OCTET_STREAM;
    }

    try {
      return MediaType.parseMediaType(contentType);
    } catch (IllegalArgumentException exception) {
      return MediaType.APPLICATION_OCTET_STREAM;
    }
  }
}
