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

import com.tbm.recruitment.matching.client.dto.ApplicationClientResponse;
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

class RecruitmentServiceClientTest {

  private static final String BASE_URL = "http://localhost:8086";

  private MockRestServiceServer mockServer;
  private RecruitmentServiceClient recruitmentServiceClient;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
    mockServer = MockRestServiceServer.bindTo(builder).build();
    recruitmentServiceClient = new RecruitmentServiceClient(builder.build());
  }

  @Test
  void sendsExpectedRequestAndParsesApplicationFields() {
    UUID applicationId = UUID.fromString("f4fca438-89f6-4f06-9574-32fa5a576165");
    UUID candidateId = UUID.fromString("850a7d5a-f72f-433f-8fa6-ec4d8e45dcc4");
    UUID jobId = UUID.fromString("6e8f5af5-7535-4d43-9856-00974d26fc79");
    UUID resumeId = UUID.fromString("5a247774-17ea-4974-a2cf-4e58df20e9ca");
    String accountId = "recruiter-account-1";
    String accountRole = "RECRUITER";
    String responseBody =
        """
        {
          "code": 1000,
          "message": "Success",
          "result": {
            "id": "%s",
            "candidateId": "%s",
            "jobId": "%s",
            "resumeId": "%s",
            "status": "SUBMITTED",
            "submittedAt": "2026-09-06T14:15:16Z"
          }
        }
        """
            .formatted(applicationId, candidateId, jobId, resumeId);

    mockServer
        .expect(requestTo(BASE_URL + "/recruitment/application/" + applicationId))
        .andExpect(method(GET))
        .andExpect(header("X-Account-Id", accountId))
        .andExpect(header("X-Account-Role", accountRole))
        .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

    ApplicationClientResponse response =
        recruitmentServiceClient.fetchRecruiterApplication(applicationId, accountId, accountRole);

    assertEquals(applicationId, response.getId());
    assertEquals(candidateId, response.getCandidateId());
    assertEquals(jobId, response.getJobId());
    assertEquals(resumeId, response.getResumeId());
    mockServer.verify();
  }

  @Test
  void rejectsNullDownstreamResult() {
    UUID applicationId = UUID.randomUUID();
    String responseBody =
        """
        {
          "code": 1000,
          "message": "Success",
          "result": null
        }
        """;

    mockServer
        .expect(requestTo(BASE_URL + "/recruitment/application/" + applicationId))
        .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () ->
                recruitmentServiceClient.fetchRecruiterApplication(
                    applicationId, "recruiter-1", "RECRUITER"));

    assertTrue(exception.getMessage().contains("empty result"));
    mockServer.verify();
  }

  @ParameterizedTest
  @MethodSource("downstreamFailureStatuses")
  void throwsClearErrorOnDownstreamHttpFailures(HttpStatus status) {
    UUID applicationId = UUID.randomUUID();

    mockServer
        .expect(requestTo(BASE_URL + "/recruitment/application/" + applicationId))
        .andRespond(withStatus(status));

    com.tbm.recruitment.matching.exception.DownstreamServiceException exception =
        assertThrows(
            com.tbm.recruitment.matching.exception.DownstreamServiceException.class,
            () ->
                recruitmentServiceClient.fetchRecruiterApplication(
                    applicationId, "recruiter-1", "RECRUITER"));

    assertTrue(
        exception.getMessage().contains("Recruitment Service returned HTTP " + status.value()));
    mockServer.verify();
  }

  @Test
  void rejectsInvalidArgumentsBeforeHttpCall() {
    mockServer.expect(
        ExpectedCount.never(), requestTo(startsWith(BASE_URL + "/recruitment/application/")));

    assertThrows(
        IllegalArgumentException.class,
        () -> recruitmentServiceClient.fetchRecruiterApplication(null, "recruiter-1", "RECRUITER"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            recruitmentServiceClient.fetchRecruiterApplication(
                UUID.randomUUID(), "   ", "RECRUITER"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            recruitmentServiceClient.fetchRecruiterApplication(
                UUID.randomUUID(), "recruiter-1", " "));

    mockServer.verify();
  }

  private static Stream<HttpStatus> downstreamFailureStatuses() {
    return Stream.of(HttpStatus.NOT_FOUND, HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
