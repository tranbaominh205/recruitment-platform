package com.tbm.recruitment.recruitment.client;

import com.tbm.recruitment.recruitment.dto.response.ApiResponse;
import com.tbm.recruitment.recruitment.dto.response.CandidateSummaryResponse;
import com.tbm.recruitment.recruitment.exception.AppException;
import com.tbm.recruitment.recruitment.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class CandidateClient {

  private final RestClient candidateRestClient;

  public CandidateClient(@Qualifier("candidateRestClient") RestClient candidateRestClient) {

    this.candidateRestClient = candidateRestClient;
  }

  public CandidateSummaryResponse getMyProfile(String accountId, String accountRole) {

    try {
      ApiResponse<CandidateSummaryResponse> response =
          candidateRestClient
              .get()
              .uri("/candidate/profile")
              .header("X-Account-Id", accountId)
              .header("X-Account-Role", accountRole)
              .retrieve()
              .onStatus(
                  status -> status.value() == 404,
                  (request, responseValue) -> {
                    throw new AppException(ErrorCode.CANDIDATE_PROFILE_NOT_FOUND);
                  })
              .onStatus(
                  HttpStatusCode::isError,
                  (request, responseValue) -> {
                    throw new AppException(ErrorCode.CANDIDATE_SERVICE_UNAVAILABLE);
                  })
              .body(new ParameterizedTypeReference<>() {});

      if (response == null || response.getResult() == null) {
        throw new AppException(ErrorCode.CANDIDATE_PROFILE_NOT_FOUND);
      }

      return response.getResult();

    } catch (AppException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new AppException(ErrorCode.CANDIDATE_SERVICE_UNAVAILABLE, exception);
    }
  }
}
