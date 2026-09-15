package com.tbm.recruitment.matching.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tbm.recruitment.matching.document.MatchResult;
import com.tbm.recruitment.matching.dto.response.AdminMatchingStatisticsResponse;
import com.tbm.recruitment.matching.dto.response.MatchResultResponse;
import com.tbm.recruitment.matching.dto.response.PageResponse;
import com.tbm.recruitment.matching.exception.AppException;
import com.tbm.recruitment.matching.exception.ErrorCode;
import com.tbm.recruitment.matching.repository.MatchResultRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;

@ExtendWith(MockitoExtension.class)
class AdminMatchingServiceTest {

  @Mock private MatchResultRepository matchResultRepository;
  @Mock private MongoTemplate mongoTemplate;

  private AdminMatchingService service;

  @BeforeEach
  void setUp() {
    service = new AdminMatchingService(matchResultRepository, mongoTemplate);
  }

  @Test
  void adminCanListStoredResultsWithPagination() {
    MatchResult result = buildResult();
    when(mongoTemplate.count(any(), org.mockito.ArgumentMatchers.eq(MatchResult.class)))
        .thenReturn(1L);
    when(mongoTemplate.find(any(), org.mockito.ArgumentMatchers.eq(MatchResult.class)))
        .thenReturn(List.of(result));

    PageResponse<MatchResultResponse> response =
        service.getResults(
            UUID.randomUUID().toString(), "ADMIN", result.getJobId(), null, null, 0, 20);

    assertEquals(1, response.content().size());
    assertEquals(result.getApplicationId(), response.content().getFirst().applicationId());
  }

  @Test
  void nonAdminRolesAreForbiddenForAdminResultRead() {
    String accountId = UUID.randomUUID().toString();

    AppException exception =
        assertThrows(AppException.class, () -> service.getStatistics(accountId, "RECRUITER"));

    assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());

    AppException candidateException =
        assertThrows(AppException.class, () -> service.getStatistics(accountId, "CANDIDATE"));

    assertEquals(ErrorCode.FORBIDDEN, candidateException.getErrorCode());
  }

  @Test
  void adminCanReadStatistics() {
    when(matchResultRepository.count()).thenReturn(5L);

    AdminMatchingStatisticsResponse response =
        service.getStatistics(UUID.randomUUID().toString(), "ADMIN");

    assertEquals(5L, response.totalMatchResults());
    verify(matchResultRepository).count();
  }

  @Test
  void adminCanReadDetailByApplicationId() {
    MatchResult result = buildResult();
    when(matchResultRepository.findById(result.getApplicationId())).thenReturn(Optional.of(result));

    MatchResultResponse response =
        service.getResultByApplicationId(
            result.getApplicationId(), UUID.randomUUID().toString(), "ADMIN");

    assertEquals(result.getApplicationId(), response.applicationId());
  }

  private MatchResult buildResult() {
    return MatchResult.builder()
        .applicationId(UUID.randomUUID())
        .candidateId(UUID.randomUUID())
        .jobId(UUID.randomUUID())
        .resumeId(UUID.randomUUID())
        .totalScore(90.0)
        .skillsScore(50.0)
        .experienceScore(20.0)
        .educationScore(10.0)
        .titleScore(5.0)
        .domainScore(5.0)
        .matchedSkills(List.of("Java"))
        .missingSkills(List.of("Kubernetes"))
        .scoringVersion("deterministic-v1")
        .scoredAt(Instant.now())
        .build();
  }
}
