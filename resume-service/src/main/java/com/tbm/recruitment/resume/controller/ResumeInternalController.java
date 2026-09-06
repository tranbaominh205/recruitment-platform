package com.tbm.recruitment.resume.controller;

import com.tbm.recruitment.resume.dto.response.ResumeDownloadResponse;
import com.tbm.recruitment.resume.service.ResumeService;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ResumeInternalController {

  ResumeService resumeService;

  @GetMapping("/resume/{resumeId}/content")
  public ResponseEntity<byte[]> getResumeContent(@PathVariable UUID resumeId) {

    ResumeDownloadResponse result = resumeService.getResumeContentForInternalUse(resumeId);

    ContentDisposition contentDisposition =
        ContentDisposition.inline().filename(result.fileName()).build();

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
