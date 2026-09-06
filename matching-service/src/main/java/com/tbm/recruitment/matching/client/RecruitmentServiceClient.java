package com.tbm.recruitment.matching.client;

import com.tbm.recruitment.matching.client.dto.ApplicationClientResponse;
import com.tbm.recruitment.matching.client.dto.ServiceApiResponse;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class RecruitmentServiceClient {

  private static final ParameterizedTypeReference<ServiceApiResponse<ApplicationClientResponse>>
      APPLICATION_RESPONSE_TYPE = new ParameterizedTypeReference<>() {};

  private final RestClient recruitmentRestClient;

  public RecruitmentServiceClient(
      @Qualifier("recruitmentRestClient") RestClient recruitmentRestClient) {
    this.recruitmentRestClient = recruitmentRestClient;
  }

  public ApplicationClientResponse fetchRecruiterApplication(
      UUID applicationId, String accountId, String accountRole) {
    validateArguments(applicationId, accountId, accountRole);

    ServiceApiResponse<ApplicationClientResponse> response;
    try {
      response =
          recruitmentRestClient
              .get()
              .uri("/recruitment/application/{applicationId}", applicationId)
              .header("X-Account-Id", accountId)
              .header("X-Account-Role", accountRole)
              .retrieve()
              .onStatus(
                  HttpStatusCode::isError,
                  (request, downstreamResponse) -> {
                    throw new IllegalStateException(
                        "Recruitment Service returned HTTP "
                            + downstreamResponse.getStatusCode().value());
                  })
              .body(APPLICATION_RESPONSE_TYPE);
    } catch (RestClientException exception) {
      throw new IllegalStateException(
          "Failed to fetch application from Recruitment Service for applicationId=" + applicationId,
          exception);
    }

    if (response == null || response.getResult() == null) {
      throw new IllegalStateException(
          "Recruitment Service returned empty result for applicationId=" + applicationId);
    }

    return response.getResult();
  }

  private void validateArguments(UUID applicationId, String accountId, String accountRole) {
    if (applicationId == null) {
      throw new IllegalArgumentException("applicationId is required");
    }
    if (accountId == null || accountId.isBlank()) {
      throw new IllegalArgumentException("accountId is required");
    }
    if (accountRole == null || accountRole.isBlank()) {
      throw new IllegalArgumentException("accountRole is required");
    }
  }
}
