package com.tbm.recruitment.candidate.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.tbm.recruitment.candidate.client.dto.JobSearchJobResponse;
import com.tbm.recruitment.candidate.exception.AppException;
import com.tbm.recruitment.candidate.exception.ErrorCode;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class JobClientTest {

  private static final String BASE_URL = "http://localhost:8084";

  private MockRestServiceServer mockServer;
  private JobClient jobClient;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
    mockServer = MockRestServiceServer.bindTo(builder).build();
    jobClient = new JobClient(builder.build());
  }

  @Test
  void fetchAllJobsIteratesThroughAllDownstreamPages() {
    String firstId = UUID.fromString("f2473d9b-6b02-4c94-b8cd-48d3f824fc89").toString();
    String secondId = UUID.fromString("642afce0-a2f3-4413-a747-5f11a05b7410").toString();

    mockServer
        .expect(requestTo(BASE_URL + "/job/search?page=0&size=100"))
        .andExpect(method(GET))
        .andRespond(
            withSuccess(
                """
                {
                  "code": 1000,
                  "message": "Success",
                  "result": {
                    "content": [
                      {
                        "id": "%s",
                        "companyId": "f734f3a0-6a95-495a-88c2-65f5e4cce293",
                        "title": "Java Engineer",
                        "description": "desc",
                        "location": "HCM",
                        "employmentType": "FULL_TIME",
                        "workplaceType": "HYBRID",
                        "status": "PUBLISHED",
                        "moderationStatus": "ACTIVE",
                        "createdAt": "2026-09-15T10:00:00Z"
                      }
                    ],
                    "page": 0,
                    "size": 100,
                    "totalElements": 2,
                    "totalPages": 2
                  }
                }
                """
                    .formatted(firstId),
                MediaType.APPLICATION_JSON));

    mockServer
        .expect(requestTo(BASE_URL + "/job/search?page=1&size=100"))
        .andExpect(method(GET))
        .andRespond(
            withSuccess(
                """
                {
                  "code": 1000,
                  "message": "Success",
                  "result": {
                    "content": [
                      {
                        "id": "%s",
                        "companyId": "f734f3a0-6a95-495a-88c2-65f5e4cce293",
                        "title": "Go Engineer",
                        "description": "desc",
                        "location": "HCM",
                        "employmentType": "FULL_TIME",
                        "workplaceType": "REMOTE",
                        "status": "PUBLISHED",
                        "moderationStatus": "ACTIVE",
                        "createdAt": "2026-09-16T10:00:00Z"
                      }
                    ],
                    "page": 1,
                    "size": 100,
                    "totalElements": 2,
                    "totalPages": 2
                  }
                }
                """
                    .formatted(secondId),
                MediaType.APPLICATION_JSON));

    List<JobSearchJobResponse> jobs = jobClient.fetchAllJobsForRecommendation();

    assertEquals(2, jobs.size());
    assertEquals(UUID.fromString(firstId), jobs.get(0).id());
    assertEquals(UUID.fromString(secondId), jobs.get(1).id());
    mockServer.verify();
  }

  @Test
  void mapsDownstreamFailureToJobServiceUnavailable() {
    mockServer
        .expect(requestTo(BASE_URL + "/job/search?page=0&size=100"))
        .andExpect(method(GET))
        .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

    AppException exception =
        assertThrows(AppException.class, () -> jobClient.fetchAllJobsForRecommendation());

    assertEquals(ErrorCode.JOB_SERVICE_UNAVAILABLE, exception.getErrorCode());
    mockServer.verify();
  }
}
