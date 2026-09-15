package com.tbm.recruitment.candidate.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.tbm.recruitment.candidate.client.JobClient;
import com.tbm.recruitment.candidate.client.dto.JobSearchJobResponse;
import com.tbm.recruitment.candidate.dto.response.PageResponse;
import com.tbm.recruitment.candidate.dto.response.RecommendedJobResponse;
import com.tbm.recruitment.candidate.entity.CandidateProfile;
import com.tbm.recruitment.candidate.exception.AppException;
import com.tbm.recruitment.candidate.exception.ErrorCode;
import com.tbm.recruitment.candidate.repository.CandidateProfileRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CandidateJobRecommendationServiceTest {

  @Mock private CandidateProfileRepository candidateProfileRepository;
  @Mock private JobClient jobClient;

  private CandidateJobRecommendationService recommendationService;
  private UUID accountId;

  @BeforeEach
  void setUp() {
    recommendationService =
        new CandidateJobRecommendationService(candidateProfileRepository, jobClient);
    accountId = UUID.fromString("0f0d0954-cf6d-46e3-9672-9fd9a84d613b");
  }

  @Test
  void allDimensionsMatchReturnsScoreOneHundred() {
    CandidateProfile profile =
        buildProfile(
            Set.of("Senior Java Developer"),
            Set.of("Ho Chi Minh"),
            Set.of("FULL_TIME"),
            Set.of("HYBRID"));

    JobSearchJobResponse job =
        buildJob(
            UUID.fromString("6b5cf486-d8f9-4776-b1d5-a99f1da6f673"),
            "Senior Java Developer",
            "Ho Chi Minh",
            "FULL_TIME",
            "HYBRID",
            "PUBLISHED",
            "ACTIVE",
            Instant.parse("2026-09-15T10:00:00Z"));

    when(candidateProfileRepository.findByAccountId(accountId))
        .thenReturn(java.util.Optional.of(profile));
    when(jobClient.fetchAllJobsForRecommendation()).thenReturn(List.of(job));

    PageResponse<RecommendedJobResponse> response =
        recommendationService.getRecommendations(accountId.toString(), "CANDIDATE", 0, 20);

    RecommendedJobResponse result = response.content().getFirst();
    assertEquals(new BigDecimal("100.00"), result.recommendationScore());
    assertEquals(
        List.of("TITLE_MATCH", "LOCATION_MATCH", "EMPLOYMENT_TYPE_MATCH", "WORKPLACE_TYPE_MATCH"),
        result.recommendationReasons());
  }

  @Test
  void normalizationAndPartialMatchUseActiveWeightsOnly() {
    CandidateProfile profile =
        buildProfile(Set.of("  Senior   Java   Developer "), Set.of(), Set.of(), Set.of("Hybrid"));

    JobSearchJobResponse job =
        buildJob(
            UUID.fromString("8cd6ea85-759e-4d6b-96a1-2e39d6cc6d3e"),
            "Senior Java Developer Backend",
            "Da Nang",
            "PART_TIME",
            "ONSITE",
            "PUBLISHED",
            "ACTIVE",
            Instant.parse("2026-09-15T11:00:00Z"));

    when(candidateProfileRepository.findByAccountId(accountId))
        .thenReturn(java.util.Optional.of(profile));
    when(jobClient.fetchAllJobsForRecommendation()).thenReturn(List.of(job));

    PageResponse<RecommendedJobResponse> response =
        recommendationService.getRecommendations(accountId.toString(), "CANDIDATE", 0, 20);

    RecommendedJobResponse result = response.content().getFirst();
    assertEquals(new BigDecimal("72.73"), result.recommendationScore());
    assertEquals(List.of("TITLE_MATCH"), result.recommendationReasons());
  }

  @Test
  void nonMatchingDimensionsReturnZeroScore() {
    CandidateProfile profile =
        buildProfile(
            Set.of("Backend Engineer"), Set.of("Ha Noi"), Set.of("FULL_TIME"), Set.of("REMOTE"));

    JobSearchJobResponse job =
        buildJob(
            UUID.randomUUID(),
            "QA Analyst",
            "Da Nang",
            "PART_TIME",
            "ONSITE",
            "PUBLISHED",
            "ACTIVE",
            Instant.parse("2026-09-15T10:30:00Z"));

    when(candidateProfileRepository.findByAccountId(accountId))
        .thenReturn(java.util.Optional.of(profile));
    when(jobClient.fetchAllJobsForRecommendation()).thenReturn(List.of(job));

    PageResponse<RecommendedJobResponse> response =
        recommendationService.getRecommendations(accountId.toString(), "CANDIDATE", 0, 20);

    RecommendedJobResponse result = response.content().getFirst();
    assertEquals(new BigDecimal("0.00"), result.recommendationScore());
    assertEquals(List.of(), result.recommendationReasons());
  }

  @Test
  void noPreferencesReturnsEligibleJobsWithZeroScoreAndNoReasons() {
    CandidateProfile profile = buildProfile(Set.of(), Set.of(), Set.of(), Set.of());

    JobSearchJobResponse older =
        buildJob(
            UUID.randomUUID(),
            "Java Dev",
            "Ha Noi",
            "FULL_TIME",
            "HYBRID",
            "PUBLISHED",
            "ACTIVE",
            Instant.parse("2026-09-15T08:00:00Z"));
    JobSearchJobResponse newer =
        buildJob(
            UUID.randomUUID(),
            "Go Dev",
            "Ho Chi Minh",
            "FULL_TIME",
            "REMOTE",
            "PUBLISHED",
            "ACTIVE",
            Instant.parse("2026-09-15T12:00:00Z"));

    when(candidateProfileRepository.findByAccountId(accountId))
        .thenReturn(java.util.Optional.of(profile));
    when(jobClient.fetchAllJobsForRecommendation()).thenReturn(List.of(older, newer));

    PageResponse<RecommendedJobResponse> response =
        recommendationService.getRecommendations(accountId.toString(), "CANDIDATE", 0, 20);

    assertEquals(2, response.content().size());
    assertEquals(newer.id(), response.content().get(0).id());
    assertEquals(new BigDecimal("0.00"), response.content().get(0).recommendationScore());
    assertEquals(List.of(), response.content().get(0).recommendationReasons());
  }

  @Test
  void excludesHiddenAndRemovedJobsFromRecommendations() {
    CandidateProfile profile = buildProfile(Set.of("Java"), Set.of(), Set.of(), Set.of());
    JobSearchJobResponse active =
        buildJob(
            UUID.randomUUID(),
            "Java",
            "HCM",
            "FULL_TIME",
            "HYBRID",
            "PUBLISHED",
            "ACTIVE",
            Instant.now());
    JobSearchJobResponse hidden =
        buildJob(
            UUID.randomUUID(),
            "Java",
            "HCM",
            "FULL_TIME",
            "HYBRID",
            "PUBLISHED",
            "HIDDEN",
            Instant.now());
    JobSearchJobResponse removed =
        buildJob(
            UUID.randomUUID(),
            "Java",
            "HCM",
            "FULL_TIME",
            "HYBRID",
            "PUBLISHED",
            "REMOVED",
            Instant.now());

    when(candidateProfileRepository.findByAccountId(accountId))
        .thenReturn(java.util.Optional.of(profile));
    when(jobClient.fetchAllJobsForRecommendation()).thenReturn(List.of(active, hidden, removed));

    PageResponse<RecommendedJobResponse> response =
        recommendationService.getRecommendations(accountId.toString(), "CANDIDATE", 0, 20);

    assertEquals(1, response.content().size());
    assertEquals(active.id(), response.content().getFirst().id());
  }

  @Test
  void excludesDraftAndClosedJobsFromRecommendations() {
    CandidateProfile profile = buildProfile(Set.of("Java"), Set.of(), Set.of(), Set.of());
    JobSearchJobResponse published =
        buildJob(
            UUID.randomUUID(),
            "Java",
            "HCM",
            "FULL_TIME",
            "HYBRID",
            "PUBLISHED",
            "ACTIVE",
            Instant.now());
    JobSearchJobResponse draft =
        buildJob(
            UUID.randomUUID(),
            "Java",
            "HCM",
            "FULL_TIME",
            "HYBRID",
            "DRAFT",
            "ACTIVE",
            Instant.now());
    JobSearchJobResponse closed =
        buildJob(
            UUID.randomUUID(),
            "Java",
            "HCM",
            "FULL_TIME",
            "HYBRID",
            "CLOSED",
            "ACTIVE",
            Instant.now());

    when(candidateProfileRepository.findByAccountId(accountId))
        .thenReturn(java.util.Optional.of(profile));
    when(jobClient.fetchAllJobsForRecommendation()).thenReturn(List.of(published, draft, closed));

    PageResponse<RecommendedJobResponse> response =
        recommendationService.getRecommendations(accountId.toString(), "CANDIDATE", 0, 20);

    assertEquals(1, response.content().size());
    assertEquals(published.id(), response.content().getFirst().id());
  }

  @Test
  void sortsByScoreThenCreatedAtThenId() {
    CandidateProfile profile = buildProfile(Set.of("Java"), Set.of(), Set.of(), Set.of());

    JobSearchJobResponse highNew =
        buildJob(
            UUID.fromString("00000000-0000-0000-0000-000000000003"),
            "Java",
            "HCM",
            "FULL_TIME",
            "HYBRID",
            "PUBLISHED",
            "ACTIVE",
            Instant.parse("2026-09-15T12:00:00Z"));
    JobSearchJobResponse highOld =
        buildJob(
            UUID.fromString("00000000-0000-0000-0000-000000000002"),
            "Java",
            "HCM",
            "FULL_TIME",
            "HYBRID",
            "PUBLISHED",
            "ACTIVE",
            Instant.parse("2026-09-15T10:00:00Z"));
    JobSearchJobResponse low =
        buildJob(
            UUID.fromString("00000000-0000-0000-0000-000000000001"),
            "Go",
            "HCM",
            "FULL_TIME",
            "HYBRID",
            "PUBLISHED",
            "ACTIVE",
            Instant.parse("2026-09-15T14:00:00Z"));

    when(candidateProfileRepository.findByAccountId(accountId))
        .thenReturn(java.util.Optional.of(profile));
    when(jobClient.fetchAllJobsForRecommendation()).thenReturn(List.of(highOld, low, highNew));

    PageResponse<RecommendedJobResponse> response =
        recommendationService.getRecommendations(accountId.toString(), "CANDIDATE", 0, 20);

    assertEquals(
        List.of(highNew.id(), highOld.id(), low.id()),
        response.content().stream().map(RecommendedJobResponse::id).toList());
  }

  @Test
  void appliesRecommendationPagination() {
    CandidateProfile profile = buildProfile(Set.of(), Set.of(), Set.of(), Set.of());
    JobSearchJobResponse first =
        buildJob(
            UUID.fromString("00000000-0000-0000-0000-000000000001"),
            "Role 1",
            "HCM",
            "FULL_TIME",
            "HYBRID",
            "PUBLISHED",
            "ACTIVE",
            Instant.parse("2026-09-15T12:00:00Z"));
    JobSearchJobResponse second =
        buildJob(
            UUID.fromString("00000000-0000-0000-0000-000000000002"),
            "Role 2",
            "HCM",
            "FULL_TIME",
            "HYBRID",
            "PUBLISHED",
            "ACTIVE",
            Instant.parse("2026-09-15T11:00:00Z"));
    JobSearchJobResponse third =
        buildJob(
            UUID.fromString("00000000-0000-0000-0000-000000000003"),
            "Role 3",
            "HCM",
            "FULL_TIME",
            "HYBRID",
            "PUBLISHED",
            "ACTIVE",
            Instant.parse("2026-09-15T10:00:00Z"));

    when(candidateProfileRepository.findByAccountId(accountId))
        .thenReturn(java.util.Optional.of(profile));
    when(jobClient.fetchAllJobsForRecommendation()).thenReturn(List.of(first, second, third));

    PageResponse<RecommendedJobResponse> response =
        recommendationService.getRecommendations(accountId.toString(), "CANDIDATE", 1, 2);

    assertEquals(3L, response.totalElements());
    assertEquals(2, response.totalPages());
    assertEquals(1, response.content().size());
    assertEquals(third.id(), response.content().getFirst().id());
  }

  @Test
  void nonCandidateRoleIsForbidden() {
    AppException exception =
        assertThrows(
            AppException.class,
            () ->
                recommendationService.getRecommendations(accountId.toString(), "RECRUITER", 0, 20));
    assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());
  }

  @Test
  void jobServiceUnavailableIsPropagated() {
    CandidateProfile profile = buildProfile(Set.of("Java"), Set.of(), Set.of(), Set.of());
    when(candidateProfileRepository.findByAccountId(accountId))
        .thenReturn(java.util.Optional.of(profile));
    when(jobClient.fetchAllJobsForRecommendation())
        .thenThrow(new AppException(ErrorCode.JOB_SERVICE_UNAVAILABLE));

    AppException exception =
        assertThrows(
            AppException.class,
            () ->
                recommendationService.getRecommendations(accountId.toString(), "CANDIDATE", 0, 20));

    assertEquals(ErrorCode.JOB_SERVICE_UNAVAILABLE, exception.getErrorCode());
  }

  private CandidateProfile buildProfile(
      Set<String> desiredTitles,
      Set<String> preferredLocations,
      Set<String> employmentTypes,
      Set<String> workplaceTypes) {
    return CandidateProfile.builder()
        .id(UUID.randomUUID())
        .accountId(accountId)
        .fullName("Candidate")
        .desiredJobTitles(desiredTitles)
        .preferredLocations(preferredLocations)
        .employmentTypes(employmentTypes)
        .workplaceTypes(workplaceTypes)
        .build();
  }

  private JobSearchJobResponse buildJob(
      UUID id,
      String title,
      String location,
      String employmentType,
      String workplaceType,
      String status,
      String moderationStatus,
      Instant createdAt) {
    return new JobSearchJobResponse(
        id,
        UUID.fromString("9e7f0314-f22b-4075-a8b6-b0cdc6753af1"),
        title,
        "description",
        location,
        employmentType,
        workplaceType,
        status,
        moderationStatus,
        createdAt);
  }
}
