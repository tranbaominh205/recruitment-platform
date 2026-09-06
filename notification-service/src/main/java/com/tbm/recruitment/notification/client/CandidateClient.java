package com.tbm.recruitment.notification.client;

import com.tbm.recruitment.notification.dto.response.ApiResponse;
import com.tbm.recruitment.notification.dto.response.CandidateAccountResponse;
import com.tbm.recruitment.notification.exception.AppException;
import com.tbm.recruitment.notification.exception.ErrorCode;
import java.util.UUID;
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

  public CandidateAccountResponse getCandidateAccount(UUID candidateId) {

    try {
      ApiResponse<CandidateAccountResponse> response =
          candidateRestClient
              .get()
              .uri("/internal/candidate/{candidateId}/account", candidateId)
              .retrieve()
              .onStatus(
                  status -> status.value() == 404,
                  (request, responseValue) -> {
                    throw new AppException(ErrorCode.CANDIDATE_NOT_FOUND);
                  })
              .onStatus(
                  HttpStatusCode::isError,
                  (request, responseValue) -> {
                    throw new AppException(ErrorCode.CANDIDATE_SERVICE_UNAVAILABLE);
                  })
              .body(new ParameterizedTypeReference<>() {});

      if (response == null || response.getResult() == null) {
        throw new AppException(ErrorCode.CANDIDATE_NOT_FOUND);
      }

      return response.getResult();

    } catch (AppException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new AppException(ErrorCode.CANDIDATE_SERVICE_UNAVAILABLE);
    }
  }
}
