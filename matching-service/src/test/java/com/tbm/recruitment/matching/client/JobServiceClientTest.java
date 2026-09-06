package com.tbm.recruitment.matching.client;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.tbm.recruitment.matching.model.EducationLevel;
import com.tbm.recruitment.matching.model.JobMatchingCriteria;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class JobServiceClientTest {

  private static final String BASE_URL = "http://localhost:8084";

  private MockRestServiceServer mockServer;
  private JobServiceClient jobServiceClient;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
    mockServer = MockRestServiceServer.bindTo(builder).build();
    jobServiceClient = new JobServiceClient(builder.build());
  }

  @Test
  void sendsOwnershipRequestAndMapsToJobMatchingCriteria() {
    UUID jobId = UUID.fromString("ef410cfc-7dc8-44b3-b08e-bf77ce9aa3da");
    String accountId = "recruiter-account-1";
    String accountRole = "RECRUITER";
    String responseBody =
        """
        {
          "code": 1000,
          "message": "Success",
          "result": {
            "id": "%s",
            "title": "Senior Java Developer",
            "requiredSkills": ["Java", "Spring Boot"],
            "minimumYearsExperience": 3,
            "requiredEducationLevel": "BACHELOR",
            "domain": "Fintech"
          }
        }
        """
            .formatted(jobId);

    mockServer
        .expect(requestTo(BASE_URL + "/job/" + jobId + "/ownership"))
        .andExpect(method(GET))
        .andExpect(header("X-Account-Id", accountId))
        .andExpect(header("X-Account-Role", accountRole))
        .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

    JobMatchingCriteria criteria =
        jobServiceClient.fetchOwnedJobMatchingCriteria(jobId, accountId, accountRole);

    assertEquals("Senior Java Developer", criteria.title());
    assertEquals(List.of("Java", "Spring Boot"), criteria.requiredSkills());
    assertEquals(3, criteria.minimumYearsExperience());
    assertEquals(EducationLevel.BACHELOR, criteria.requiredEducationLevel());
    assertEquals("Fintech", criteria.domain());
    mockServer.verify();
  }

  @Test
  void rejectsNullResultFromDownstreamResponse() {
    UUID jobId = UUID.randomUUID();
    String responseBody =
        """
        {
          "code": 1000,
          "message": "Success",
          "result": null
        }
        """;

    mockServer
        .expect(requestTo(BASE_URL + "/job/" + jobId + "/ownership"))
        .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () ->
                jobServiceClient.fetchOwnedJobMatchingCriteria(jobId, "recruiter-1", "RECRUITER"));

    assertTrue(exception.getMessage().contains("empty result"));
    mockServer.verify();
  }

  @Test
  void rejectsIncompleteDownstreamResult() {
    UUID jobId = UUID.randomUUID();
    String responseBody =
        """
        {
          "code": 1000,
          "message": "Success",
          "result": {
            "id": "%s",
            "title": "Senior Java Developer",
            "requiredSkills": null,
            "minimumYearsExperience": 3,
            "requiredEducationLevel": "BACHELOR",
            "domain": "Fintech"
          }
        }
        """
            .formatted(jobId);

    mockServer
        .expect(requestTo(BASE_URL + "/job/" + jobId + "/ownership"))
        .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () ->
                jobServiceClient.fetchOwnedJobMatchingCriteria(jobId, "recruiter-1", "RECRUITER"));

    assertTrue(exception.getMessage().contains("missing requiredSkills"));
    mockServer.verify();
  }

  @ParameterizedTest
  @MethodSource("downstreamFailureStatuses")
  void throwsClearErrorOnDownstreamHttpFailures(HttpStatus status) {
    UUID jobId = UUID.randomUUID();

    mockServer
        .expect(requestTo(BASE_URL + "/job/" + jobId + "/ownership"))
        .andRespond(withStatus(status));

    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () ->
                jobServiceClient.fetchOwnedJobMatchingCriteria(jobId, "recruiter-1", "RECRUITER"));

    assertTrue(exception.getMessage().contains("Job Service returned HTTP " + status.value()));
    mockServer.verify();
  }

  @Test
  void rejectsInvalidArgumentsBeforeHttpCall() {
    mockServer.expect(ExpectedCount.never(), requestTo(startsWith(BASE_URL + "/job/")));

    assertThrows(
        IllegalArgumentException.class,
        () -> jobServiceClient.fetchOwnedJobMatchingCriteria(null, "recruiter-1", "RECRUITER"));
    assertThrows(
        IllegalArgumentException.class,
        () -> jobServiceClient.fetchOwnedJobMatchingCriteria(UUID.randomUUID(), " ", "RECRUITER"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            jobServiceClient.fetchOwnedJobMatchingCriteria(UUID.randomUUID(), "recruiter-1", " "));

    mockServer.verify();
  }

  private static Stream<HttpStatus> downstreamFailureStatuses() {
    return Stream.of(HttpStatus.FORBIDDEN, HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
