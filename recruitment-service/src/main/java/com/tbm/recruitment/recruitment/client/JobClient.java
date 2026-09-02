package com.tbm.recruitment.recruitment.client;

import com.tbm.recruitment.recruitment.dto.response.ApiResponse;
import com.tbm.recruitment.recruitment.dto.response.JobSummaryResponse;
import com.tbm.recruitment.recruitment.exception.AppException;
import com.tbm.recruitment.recruitment.exception.ErrorCode;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class JobClient {

  private final RestClient jobRestClient;

  public JobClient(@Qualifier("jobRestClient") RestClient jobRestClient) {

    this.jobRestClient = jobRestClient;
  }

  public JobSummaryResponse getPublishedJob(UUID jobId) {

    try {
      ApiResponse<JobSummaryResponse> response =
          jobRestClient
              .get()
              .uri("/job/{jobId}", jobId)
              .retrieve()
              .onStatus(
                  status -> status.value() == 404,
                  (request, responseValue) -> {
                    throw new AppException(ErrorCode.JOB_NOT_AVAILABLE);
                  })
              .onStatus(
                  HttpStatusCode::isError,
                  (request, responseValue) -> {
                    throw new AppException(ErrorCode.JOB_SERVICE_UNAVAILABLE);
                  })
              .body(new ParameterizedTypeReference<>() {});

      if (response == null || response.getResult() == null) {

        throw new AppException(ErrorCode.JOB_NOT_AVAILABLE);
      }

      return response.getResult();

    } catch (AppException exception) {
      throw exception;

    } catch (Exception exception) {
      throw new AppException(ErrorCode.JOB_SERVICE_UNAVAILABLE, exception);
    }
  }

  public JobSummaryResponse getOwnedJob(UUID jobId, String accountId, String accountRole) {

    try {
      ApiResponse<JobSummaryResponse> response =
          jobRestClient
              .get()
              .uri("/job/{jobId}/ownership", jobId)
              .header("X-Account-Id", accountId)
              .header("X-Account-Role", accountRole)
              .retrieve()
              .onStatus(
                  status -> status.value() == 404,
                  (request, responseValue) -> {
                    throw new AppException(ErrorCode.JOB_NOT_FOUND);
                  })
              .onStatus(
                  status -> status.value() == 401,
                  (request, responseValue) -> {
                    throw new AppException(ErrorCode.UNAUTHENTICATED);
                  })
              .onStatus(
                  status -> status.value() == 403,
                  (request, responseValue) -> {
                    throw new AppException(ErrorCode.FORBIDDEN);
                  })
              .onStatus(
                  HttpStatusCode::isError,
                  (request, responseValue) -> {
                    throw new AppException(ErrorCode.JOB_SERVICE_UNAVAILABLE);
                  })
              .body(new ParameterizedTypeReference<>() {});

      if (response == null || response.getResult() == null) {

        throw new AppException(ErrorCode.JOB_NOT_FOUND);
      }

      return response.getResult();

    } catch (AppException exception) {
      throw exception;

    } catch (Exception exception) {
      throw new AppException(ErrorCode.JOB_SERVICE_UNAVAILABLE, exception);
    }
  }
}
