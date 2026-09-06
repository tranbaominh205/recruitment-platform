package com.tbm.recruitment.matching.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.tbm.recruitment.matching.client.JobServiceClient;
import com.tbm.recruitment.matching.client.RecruitmentServiceClient;
import com.tbm.recruitment.matching.client.dto.ApplicationClientResponse;
import com.tbm.recruitment.matching.document.MatchResult;
import com.tbm.recruitment.matching.document.StructuredResume;
import com.tbm.recruitment.matching.exception.AppException;
import com.tbm.recruitment.matching.exception.DownstreamServiceException;
import com.tbm.recruitment.matching.exception.ErrorCode;
import com.tbm.recruitment.matching.extractor.MatchExplanationGenerator;
import com.tbm.recruitment.matching.model.JobMatchingCriteria;
import com.tbm.recruitment.matching.model.MatchExplanation;
import com.tbm.recruitment.matching.model.MatchScoreResult;
import com.tbm.recruitment.matching.repository.MatchResultRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApplicationMatchingServiceTest {

  @Mock private RecruitmentServiceClient recruitmentServiceClient;
  @Mock private JobServiceClient jobServiceClient;
  @Mock private ResumeAnalysisService resumeAnalysisService;
  @Mock private DeterministicMatchScorer deterministicMatchScorer;
  @Mock private MatchExplanationGenerator matchExplanationGenerator;
  @Mock private MatchResultRepository matchResultRepository;

  @InjectMocks private ApplicationMatchingService subject;

  private UUID applicationId;
  private UUID candidateId;
  private UUID jobId;
  private UUID resumeId;

  @BeforeEach
  void setUp() {
    applicationId = UUID.randomUUID();
    candidateId = UUID.randomUUID();
    jobId = UUID.randomUUID();
    resumeId = UUID.randomUUID();
  }

  @Test
  void newApplication_executesFullFlow_andPersistsOnce() {
    when(recruitmentServiceClient.fetchRecruiterApplication(
            eq(applicationId), anyString(), anyString()))
        .thenReturn(buildApplication());
    when(matchResultRepository.findById(applicationId)).thenReturn(Optional.empty());
    // Simulate normal Spring Data save: return the saved entity
    when(matchResultRepository.save(any(MatchResult.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    JobMatchingCriteria criteria = new JobMatchingCriteria("T", List.of("Java"), 2, null, "D");
    when(jobServiceClient.fetchOwnedJobMatchingCriteria(eq(jobId), anyString(), anyString()))
        .thenReturn(criteria);

    StructuredResume structured =
        StructuredResume.builder().resumeId(resumeId).skills(List.of("Java")).build();
    when(resumeAnalysisService.analyzeAndPersist(resumeId)).thenReturn(structured);

    MatchScoreResult scoreResult =
        new MatchScoreResult(90.0, 55.0, 20.0, 10.0, 3.0, 2.0, List.of("Java"), List.of());
    when(deterministicMatchScorer.score(structured, criteria)).thenReturn(scoreResult);

    MatchExplanation explanation =
        new MatchExplanation(
            "Strong Java match with a clear backend fit.",
            List.of("Java skills", "Backend experience"),
            List.of("Limited AWS breadth"),
            "gemini-3.5-flash",
            Instant.now());
    when(matchExplanationGenerator.generate(criteria, structured, scoreResult))
        .thenReturn(explanation);

    var response = subject.matchApplication(applicationId, "recruiter-1", "RECRUITER");
    assertEquals(explanation, response.explanation());

    ArgumentCaptor<MatchResult> savedCaptor = ArgumentCaptor.forClass(MatchResult.class);
    verify(matchResultRepository, times(1)).save(savedCaptor.capture());
    MatchResult saved = savedCaptor.getValue();
    assertEquals(applicationId, saved.getApplicationId());
    assertEquals(candidateId, saved.getCandidateId());
    assertEquals(jobId, saved.getJobId());
    assertEquals(resumeId, saved.getResumeId());
    assertEquals(90.0, saved.getTotalScore());
    assertEquals(explanation, saved.getExplanation());
  }

  @Test
  void existingMatchResult_returnsCached_afterAuthorization() {
    when(recruitmentServiceClient.fetchRecruiterApplication(
            eq(applicationId), anyString(), anyString()))
        .thenReturn(buildApplication());

    MatchExplanation explanation =
        new MatchExplanation(
            "Existing explanation.",
            List.of("Match"),
            List.of("Gap"),
            "gemini-3.5-flash",
            Instant.now());
    MatchResult cached =
        MatchResult.builder()
            .applicationId(applicationId)
            .candidateId(candidateId)
            .jobId(jobId)
            .resumeId(resumeId)
            .totalScore(42.0)
            .skillsScore(30.0)
            .experienceScore(5.0)
            .educationScore(2.0)
            .titleScore(3.0)
            .domainScore(2.0)
            .matchedSkills(List.of("Java"))
            .missingSkills(List.of())
            .scoringVersion("deterministic-v1")
            .scoredAt(Instant.now())
            .explanation(explanation)
            .build();

    when(matchResultRepository.findById(applicationId)).thenReturn(Optional.of(cached));

    var response = subject.matchApplication(applicationId, "recruiter-1", "RECRUITER");
    assertEquals(applicationId, response.applicationId());
    assertEquals(42.0, response.totalScore());
    assertEquals(explanation, response.explanation());

    verify(jobServiceClient, never())
        .fetchOwnedJobMatchingCriteria(any(), anyString(), anyString());
    verify(resumeAnalysisService, never()).analyzeAndPersist(any());
    verify(deterministicMatchScorer, never()).score(any(), any());
    verify(matchExplanationGenerator, never()).generate(any(), any(), any());
    verify(matchResultRepository, never()).save(any()); // not saved again
  }

  @Test
  void candidateRoleForbidden() {
    AppException ex =
        assertThrows(
            AppException.class, () -> subject.matchApplication(applicationId, "acct", "CANDIDATE"));
    assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
  }

  @Test
  void missingAccountIdentityRejected() {
    AppException ex =
        assertThrows(
            AppException.class, () -> subject.matchApplication(applicationId, " ", "RECRUITER"));
    assertEquals(ErrorCode.UNAUTHENTICATED, ex.getErrorCode());
  }

  @Test
  void applicationMissingResumeId_rejected() {
    ApplicationClientResponse app = buildApplication();
    app.setResumeId(null);
    when(recruitmentServiceClient.fetchRecruiterApplication(
            eq(applicationId), anyString(), anyString()))
        .thenReturn(app);
    AppException ex =
        assertThrows(
            AppException.class,
            () -> subject.matchApplication(applicationId, "recruiter-1", "RECRUITER"));
    assertEquals(ErrorCode.INTERNAL_SERVER_ERROR, ex.getErrorCode());
    verify(matchResultRepository, never()).save(any());
  }

  @Test
  void resumeAnalysisFailure_noSave() {
    when(recruitmentServiceClient.fetchRecruiterApplication(
            eq(applicationId), anyString(), anyString()))
        .thenReturn(buildApplication());
    when(matchResultRepository.findById(applicationId)).thenReturn(Optional.empty());
    when(jobServiceClient.fetchOwnedJobMatchingCriteria(eq(jobId), anyString(), anyString()))
        .thenReturn(new JobMatchingCriteria("T", List.of("Java"), 2, null, "D"));
    when(resumeAnalysisService.analyzeAndPersist(resumeId))
        .thenThrow(new RuntimeException("AI down"));

    assertThrows(
        AppException.class,
        () -> subject.matchApplication(applicationId, "recruiter-1", "RECRUITER"));
    verify(matchResultRepository, never()).save(any());
  }

  @Test
  void explanationGenerationFailure_throwsDependencyUnavailable_andDoesNotSave() {
    when(recruitmentServiceClient.fetchRecruiterApplication(
            eq(applicationId), anyString(), anyString()))
        .thenReturn(buildApplication());
    when(matchResultRepository.findById(applicationId)).thenReturn(Optional.empty());
    when(jobServiceClient.fetchOwnedJobMatchingCriteria(eq(jobId), anyString(), anyString()))
        .thenReturn(new JobMatchingCriteria("T", List.of("Java"), 2, null, "D"));

    StructuredResume structured =
        StructuredResume.builder().resumeId(resumeId).skills(List.of("Java")).build();
    when(resumeAnalysisService.analyzeAndPersist(resumeId)).thenReturn(structured);
    MatchScoreResult scoreResult =
        new MatchScoreResult(80.0, 55.0, 20.0, 5.0, 0.0, 0.0, List.of("Java"), List.of());
    when(deterministicMatchScorer.score(eq(structured), any(JobMatchingCriteria.class)))
        .thenReturn(scoreResult);
    when(matchExplanationGenerator.generate(any(), any(), any()))
        .thenThrow(new RuntimeException("explanation unavailable"));

    AppException exception =
        assertThrows(
            AppException.class,
            () -> subject.matchApplication(applicationId, "recruiter-1", "RECRUITER"));

    assertEquals(ErrorCode.DEPENDENCY_UNAVAILABLE, exception.getErrorCode());
    verify(matchResultRepository, never()).save(any());
  }

  @Test
  void scorerFailure_noSave() {
    when(recruitmentServiceClient.fetchRecruiterApplication(
            eq(applicationId), anyString(), anyString()))
        .thenReturn(buildApplication());
    when(matchResultRepository.findById(applicationId)).thenReturn(Optional.empty());
    when(jobServiceClient.fetchOwnedJobMatchingCriteria(eq(jobId), anyString(), anyString()))
        .thenReturn(new JobMatchingCriteria("T", List.of("Java"), 2, null, "D"));
    StructuredResume structured =
        StructuredResume.builder().resumeId(resumeId).skills(List.of("Java")).build();
    when(resumeAnalysisService.analyzeAndPersist(resumeId)).thenReturn(structured);
    when(deterministicMatchScorer.score(structured, null))
        .thenThrow(new RuntimeException("scorer fail"));

    assertThrows(
        AppException.class,
        () -> subject.matchApplication(applicationId, "recruiter-1", "RECRUITER"));
    verify(matchResultRepository, never()).save(any());
  }

  @Test
  void getExisting_performsAuthorizationAndReturns() {
    when(recruitmentServiceClient.fetchRecruiterApplication(
            eq(applicationId), anyString(), anyString()))
        .thenReturn(buildApplication());

    MatchExplanation explanation =
        new MatchExplanation(
            "Stored explanation.",
            List.of("Skill"),
            List.of("Gap"),
            "gemini-3.5-flash",
            Instant.now());
    MatchResult cached =
        MatchResult.builder()
            .applicationId(applicationId)
            .candidateId(candidateId)
            .jobId(jobId)
            .resumeId(resumeId)
            .totalScore(55.0)
            .scoringVersion("deterministic-v1")
            .scoredAt(Instant.now())
            .explanation(explanation)
            .build();
    when(matchResultRepository.findById(applicationId)).thenReturn(Optional.of(cached));

    var resp = subject.getMatchResult(applicationId, "recruiter-1", "RECRUITER");
    assertEquals(applicationId, resp.applicationId());
    assertEquals(explanation, resp.explanation());

    verify(jobServiceClient, never())
        .fetchOwnedJobMatchingCriteria(any(), anyString(), anyString());
    verify(resumeAnalysisService, never()).analyzeAndPersist(any());
    verify(deterministicMatchScorer, never()).score(any(), any());
    verify(matchExplanationGenerator, never()).generate(any(), any(), any());
  }

  @Test
  void legacyMatchResultWithNullExplanation_returnsWithoutBackfill() {
    when(recruitmentServiceClient.fetchRecruiterApplication(
            eq(applicationId), anyString(), anyString()))
        .thenReturn(buildApplication());

    MatchResult cached =
        MatchResult.builder()
            .applicationId(applicationId)
            .candidateId(candidateId)
            .jobId(jobId)
            .resumeId(resumeId)
            .totalScore(55.0)
            .scoringVersion("deterministic-v1")
            .scoredAt(Instant.now())
            .build();
    when(matchResultRepository.findById(applicationId)).thenReturn(Optional.of(cached));

    var resp = subject.getMatchResult(applicationId, "recruiter-1", "RECRUITER");
    assertNull(resp.explanation());
    verify(matchExplanationGenerator, never()).generate(any(), any(), any());
  }

  @Test
  void getMissing_throwsMatchNotFound() {
    when(recruitmentServiceClient.fetchRecruiterApplication(
            eq(applicationId), anyString(), anyString()))
        .thenReturn(buildApplication());
    when(matchResultRepository.findById(applicationId)).thenReturn(Optional.empty());
    AppException ex =
        assertThrows(
            AppException.class,
            () -> subject.getMatchResult(applicationId, "recruiter-1", "RECRUITER"));
    assertEquals(ErrorCode.MATCH_RESULT_NOT_FOUND, ex.getErrorCode());
  }

  @Test
  void cachedResultNotRevealedWhenAuthorizationFails() {
    when(recruitmentServiceClient.fetchRecruiterApplication(
            eq(applicationId), anyString(), anyString()))
        .thenThrow(new DownstreamServiceException("Recruitment Service", 403, "forbidden"));
    MatchResult cached =
        MatchResult.builder()
            .applicationId(applicationId)
            .candidateId(candidateId)
            .jobId(jobId)
            .resumeId(resumeId)
            .totalScore(55.0)
            .build();
    // Do not stub or access matchResultRepository: authorization must fail before repository is
    // consulted

    AppException ex =
        assertThrows(
            AppException.class,
            () -> subject.matchApplication(applicationId, "recruiter-1", "RECRUITER"));
    assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());

    // Verify repository and downstream services are not called when authorization fails
    verify(matchResultRepository, never()).findById(any());
    verify(matchResultRepository, never()).save(any());
    verify(jobServiceClient, never())
        .fetchOwnedJobMatchingCriteria(any(), anyString(), anyString());
    verify(resumeAnalysisService, never()).analyzeAndPersist(any());
    verify(deterministicMatchScorer, never()).score(any(), any());
  }

  private ApplicationClientResponse buildApplication() {
    ApplicationClientResponse app = new ApplicationClientResponse();
    app.setId(applicationId);
    app.setCandidateId(candidateId);
    app.setJobId(jobId);
    app.setResumeId(resumeId);
    return app;
  }
}
