package com.tbm.recruitment.candidate.client;

import com.tbm.recruitment.candidate.client.dto.JobSearchJobResponse;
import com.tbm.recruitment.candidate.dto.response.ApiResponse;
import com.tbm.recruitment.candidate.dto.response.PageResponse;
import com.tbm.recruitment.candidate.exception.AppException;
import com.tbm.recruitment.candidate.exception.ErrorCode;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class JobClient {

  static final int DOWNSTREAM_PAGE_SIZE = 100;
  static final ParameterizedTypeReference<ApiResponse<PageResponse<JobSearchJobResponse>>>
      JOB_SEARCH_RESPONSE_TYPE = new ParameterizedTypeReference<>() {};

  @Qualifier("jobRestClient")
  RestClient jobRestClient;

  public List<JobSearchJobResponse> fetchAllJobsForRecommendation() {
    PageResponse<JobSearchJobResponse> firstPage = fetchSearchPage(0, DOWNSTREAM_PAGE_SIZE);
    List<JobSearchJobResponse> results = new ArrayList<>(safeContent(firstPage.content()));

    for (int page = 1; page < firstPage.totalPages(); page++) {
      PageResponse<JobSearchJobResponse> currentPage = fetchSearchPage(page, DOWNSTREAM_PAGE_SIZE);
      results.addAll(safeContent(currentPage.content()));
    }

    return List.copyOf(results);
  }

  private PageResponse<JobSearchJobResponse> fetchSearchPage(int page, int size) {
    try {
      ApiResponse<PageResponse<JobSearchJobResponse>> response =
          jobRestClient
              .get()
              .uri(
                  uriBuilder ->
                      uriBuilder
                          .path("/job/search")
                          .queryParam("page", page)
                          .queryParam("size", size)
                          .build())
              .retrieve()
              .onStatus(
                  HttpStatusCode::isError,
                  (request, downstreamResponse) -> {
                    throw new AppException(ErrorCode.JOB_SERVICE_UNAVAILABLE);
                  })
              .body(JOB_SEARCH_RESPONSE_TYPE);

      if (response == null || response.getResult() == null) {
        throw new AppException(ErrorCode.JOB_SERVICE_UNAVAILABLE);
      }

      return response.getResult();
    } catch (AppException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new AppException(ErrorCode.JOB_SERVICE_UNAVAILABLE);
    }
  }

  private List<JobSearchJobResponse> safeContent(List<JobSearchJobResponse> content) {
    if (content == null || content.isEmpty()) {
      return List.of();
    }

    return content;
  }
}
