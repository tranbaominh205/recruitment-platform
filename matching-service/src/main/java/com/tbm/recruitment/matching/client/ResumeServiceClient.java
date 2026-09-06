package com.tbm.recruitment.matching.client;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ResumeServiceClient {

  private final RestClient resumeRestClient;

  public ResumeServiceClient(@Qualifier("resumeRestClient") RestClient resumeRestClient) {
    this.resumeRestClient = resumeRestClient;
  }

  public byte[] fetchPdfContent(UUID resumeId) {
    if (resumeId == null) {
      throw new IllegalArgumentException("resumeId is required");
    }

    try {
      byte[] content =
          resumeRestClient
              .get()
              .uri("/internal/resume/{resumeId}/content", resumeId)
              .retrieve()
              .onStatus(
                  HttpStatusCode::isError,
                  (request, response) -> {
                    throw new IllegalStateException(
                        "Resume Service returned HTTP " + response.getStatusCode().value());
                  })
              .body(byte[].class);

      if (content == null || content.length == 0) {
        throw new IllegalStateException(
            "Resume Service returned empty content for resumeId=" + resumeId);
      }

      return content;
    } catch (IllegalStateException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new IllegalStateException(
          "Failed to fetch resume content for resumeId=" + resumeId, exception);
    }
  }
}
