package com.tbm.recruitment.matching.client;

import com.tbm.recruitment.matching.client.dto.JobClientResponse;
import com.tbm.recruitment.matching.client.dto.ServiceApiResponse;
import com.tbm.recruitment.matching.model.JobMatchingCriteria;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class JobServiceClient {

  private static final ParameterizedTypeReference<ServiceApiResponse<JobClientResponse>>
      JOB_RESPONSE_TYPE = new ParameterizedTypeReference<>() {};

  private final RestClient jobRestClient;

  public JobServiceClient(@Qualifier("jobRestClient") RestClient jobRestClient) {
    this.jobRestClient = jobRestClient;
  }

  public JobMatchingCriteria fetchOwnedJobMatchingCriteria(
      UUID jobId, String accountId, String accountRole) {
    validateArguments(jobId, accountId, accountRole);

    ServiceApiResponse<JobClientResponse> response;
    try {
      response =
          jobRestClient
              .get()
              .uri("/job/{jobId}/ownership", jobId)
              .header("X-Account-Id", accountId)
              .header("X-Account-Role", accountRole)
              .retrieve()
              .onStatus(
                  HttpStatusCode::isError,
                  (request, downstreamResponse) -> {
                    throw new IllegalStateException(
                        "Job Service returned HTTP " + downstreamResponse.getStatusCode().value());
                  })
              .body(JOB_RESPONSE_TYPE);
    } catch (RestClientException exception) {
      throw new IllegalStateException(
          "Failed to fetch owned job details from Job Service for jobId=" + jobId, exception);
    }

    if (response == null || response.getResult() == null) {
      throw new IllegalStateException("Job Service returned empty result for jobId=" + jobId);
    }

    return mapToMatchingCriteria(response.getResult());
  }

  private JobMatchingCriteria mapToMatchingCriteria(JobClientResponse jobResponse) {
    if (jobResponse.getId() == null) {
      throw new IllegalStateException("Job Service response missing id");
    }
    if (jobResponse.getTitle() == null || jobResponse.getTitle().isBlank()) {
      throw new IllegalStateException("Job Service response missing title");
    }
    if (jobResponse.getRequiredSkills() == null || jobResponse.getRequiredSkills().isEmpty()) {
      throw new IllegalStateException("Job Service response missing requiredSkills");
    }
    if (jobResponse.getMinimumYearsExperience() == null) {
      throw new IllegalStateException("Job Service response missing minimumYearsExperience");
    }
    if (jobResponse.getRequiredEducationLevel() == null) {
      throw new IllegalStateException("Job Service response missing requiredEducationLevel");
    }
    if (jobResponse.getDomain() == null || jobResponse.getDomain().isBlank()) {
      throw new IllegalStateException("Job Service response missing domain");
    }

    List<String> normalizedSkills = new ArrayList<>();
    for (String requiredSkill : jobResponse.getRequiredSkills()) {
      if (requiredSkill == null || requiredSkill.isBlank()) {
        throw new IllegalStateException("Job Service response contains blank required skill");
      }
      normalizedSkills.add(requiredSkill);
    }

    return new JobMatchingCriteria(
        jobResponse.getTitle(),
        List.copyOf(normalizedSkills),
        jobResponse.getMinimumYearsExperience(),
        jobResponse.getRequiredEducationLevel(),
        jobResponse.getDomain());
  }

  private void validateArguments(UUID jobId, String accountId, String accountRole) {
    if (jobId == null) {
      throw new IllegalArgumentException("jobId is required");
    }
    if (accountId == null || accountId.isBlank()) {
      throw new IllegalArgumentException("accountId is required");
    }
    if (accountRole == null || accountRole.isBlank()) {
      throw new IllegalArgumentException("accountRole is required");
    }
  }
}
