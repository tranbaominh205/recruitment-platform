package com.tbm.recruitment.job.client;

import com.tbm.recruitment.job.dto.response.ApiResponse;
import com.tbm.recruitment.job.dto.response.CompanySummaryResponse;
import com.tbm.recruitment.job.exception.AppException;
import com.tbm.recruitment.job.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EmployerClient {

  RestClient employerRestClient;

  public CompanySummaryResponse getMyCompany(String accountId, String accountRole) {
    try {
      ApiResponse<CompanySummaryResponse> response =
          employerRestClient
              .get()
              .uri("/employer/company")
              .header("X-Account-Id", accountId)
              .header("X-Account-Role", accountRole)
              .retrieve()
              .onStatus(
                  status -> status.value() == 404,
                  (request, responseValue) -> {
                    throw new AppException(ErrorCode.COMPANY_NOT_FOUND);
                  })
              .onStatus(
                  HttpStatusCode::isError,
                  (request, responseValue) -> {
                    throw new AppException(ErrorCode.EMPLOYER_SERVICE_UNAVAILABLE);
                  })
              .body(new ParameterizedTypeReference<>() {});

      if (response == null || response.getResult() == null) {
        throw new AppException(ErrorCode.COMPANY_NOT_FOUND);
      }

      return response.getResult();

    } catch (AppException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new AppException(ErrorCode.EMPLOYER_SERVICE_UNAVAILABLE);
    }
  }
}
