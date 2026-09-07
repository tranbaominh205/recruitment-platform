package com.tbm.recruitment.recruitment.client;

import com.tbm.recruitment.recruitment.dto.response.ApiResponse;
import com.tbm.recruitment.recruitment.dto.response.ResumeSummaryResponse;
import com.tbm.recruitment.recruitment.dto.response.SubmittedResumeContent;
import com.tbm.recruitment.recruitment.exception.AppException;
import com.tbm.recruitment.recruitment.exception.ErrorCode;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ResumeClient {

  private final RestClient resumeRestClient;

  public ResumeClient(@Qualifier("resumeRestClient") RestClient resumeRestClient) {

    this.resumeRestClient = resumeRestClient;
  }

  public ResumeSummaryResponse getMyResume(UUID resumeId, String accountId, String accountRole) {

    try {
      ApiResponse<ResumeSummaryResponse> response =
          resumeRestClient
              .get()
              .uri("/resume/{resumeId}", resumeId)
              .header("X-Account-Id", accountId)
              .header("X-Account-Role", accountRole)
              .retrieve()
              .onStatus(
                  status -> status.value() == 404,
                  (request, responseValue) -> {
                    throw new AppException(ErrorCode.RESUME_NOT_FOUND);
                  })
              .onStatus(
                  HttpStatusCode::isError,
                  (request, responseValue) -> {
                    throw new AppException(ErrorCode.RESUME_SERVICE_UNAVAILABLE);
                  })
              .body(new ParameterizedTypeReference<>() {});

      if (response == null || response.getResult() == null) {
        throw new AppException(ErrorCode.RESUME_NOT_FOUND);
      }

      return response.getResult();

    } catch (AppException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new AppException(ErrorCode.RESUME_SERVICE_UNAVAILABLE, exception);
    }
  }

  public SubmittedResumeContent getResumeContent(UUID resumeId) {
    try {
      var response =
          resumeRestClient
              .get()
              .uri("/internal/resume/{resumeId}/content", resumeId)
              .retrieve()
              .onStatus(
                  status -> status.value() == 404,
                  (request, responseValue) -> {
                    throw new AppException(ErrorCode.RESUME_NOT_FOUND);
                  })
              .onStatus(
                  HttpStatusCode::isError,
                  (request, responseValue) -> {
                    throw new AppException(ErrorCode.RESUME_SERVICE_UNAVAILABLE);
                  })
              .toEntity(byte[].class);

      byte[] content = response.getBody();
      if (content == null) {
        throw new AppException(ErrorCode.RESUME_NOT_FOUND);
      }

      String fileName = "submitted-resume.pdf";
      String contentDisposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
      if (contentDisposition != null) {
        try {
          String candidate =
              org.springframework.http.ContentDisposition.parse(contentDisposition).getFilename();
          if (candidate != null && !candidate.isBlank()) {
            fileName = candidate;
          }
        } catch (IllegalArgumentException ignored) {
          // Keep the safe fallback filename.
        }
      }

      String contentType =
          response.getHeaders().getContentType() == null
              ? "application/octet-stream"
              : response.getHeaders().getContentType().toString();
      return new SubmittedResumeContent(fileName, contentType, content);
    } catch (AppException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new AppException(ErrorCode.RESUME_SERVICE_UNAVAILABLE, exception);
    }
  }
}
