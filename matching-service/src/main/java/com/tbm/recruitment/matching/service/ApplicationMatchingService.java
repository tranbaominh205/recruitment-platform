package com.tbm.recruitment.matching.service;

import com.tbm.recruitment.matching.client.JobServiceClient;
import com.tbm.recruitment.matching.client.RecruitmentServiceClient;
import com.tbm.recruitment.matching.client.dto.ApplicationClientResponse;
import com.tbm.recruitment.matching.document.MatchResult;
import com.tbm.recruitment.matching.document.StructuredResume;
import com.tbm.recruitment.matching.dto.response.MatchResultResponse;
import com.tbm.recruitment.matching.exception.AppException;
import com.tbm.recruitment.matching.exception.DownstreamServiceException;
import com.tbm.recruitment.matching.exception.ErrorCode;
import com.tbm.recruitment.matching.extractor.MatchExplanationGenerator;
import com.tbm.recruitment.matching.model.JobMatchingCriteria;
import com.tbm.recruitment.matching.model.MatchExplanation;
import com.tbm.recruitment.matching.model.MatchScoreResult;
import com.tbm.recruitment.matching.repository.MatchResultRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ApplicationMatchingService {

  private static final String RECRUITER_ROLE = "RECRUITER";
  private static final String DETERMINISTIC_SCORING_VERSION = "deterministic-v1";

  private final RecruitmentServiceClient recruitmentServiceClient;
  private final JobServiceClient jobServiceClient;
  private final ResumeAnalysisService resumeAnalysisService;
  private final DeterministicMatchScorer deterministicMatchScorer;
  private final MatchExplanationGenerator matchExplanationGenerator;
  private final MatchResultRepository matchResultRepository;

  public MatchResultResponse matchApplication(
      UUID applicationId, String accountId, String accountRole) {
    validateRecruiterRequest(applicationId, accountId, accountRole);

    ApplicationClientResponse application =
        fetchAndValidateAuthorizedApplication(applicationId, accountId, accountRole);

    Optional<MatchResult> existing = matchResultRepository.findById(applicationId);
    if (existing.isPresent()) {
      MatchResult cached = existing.get();
      assertStoredIdsMatchApplication(cached, application);
      return toResponse(cached);
    }

    JobMatchingCriteria criteria =
        fetchOwnedJobMatchingCriteria(application.getJobId(), accountId, accountRole);
    StructuredResume structuredResume = analyzeResume(application.getResumeId());
    MatchScoreResult scoreResult = score(structuredResume, criteria);
    MatchExplanation explanation = generateExplanation(criteria, structuredResume, scoreResult);

    MatchResult toSave =
        MatchResult.builder()
            .applicationId(application.getId())
            .candidateId(application.getCandidateId())
            .jobId(application.getJobId())
            .resumeId(application.getResumeId())
            .totalScore(scoreResult.totalScore())
            .skillsScore(scoreResult.skillsScore())
            .experienceScore(scoreResult.experienceScore())
            .educationScore(scoreResult.educationScore())
            .titleScore(scoreResult.titleScore())
            .domainScore(scoreResult.domainScore())
            .matchedSkills(scoreResult.matchedSkills())
            .missingSkills(scoreResult.missingSkills())
            .scoringVersion(DETERMINISTIC_SCORING_VERSION)
            .scoredAt(Instant.now())
            .explanation(explanation)
            .build();

    MatchResult saved = matchResultRepository.save(toSave);
    return toResponse(saved);
  }

  public MatchResultResponse getMatchResult(
      UUID applicationId, String accountId, String accountRole) {
    validateRecruiterRequest(applicationId, accountId, accountRole);

    ApplicationClientResponse application =
        fetchAndValidateAuthorizedApplication(applicationId, accountId, accountRole);

    MatchResult stored =
        matchResultRepository
            .findById(applicationId)
            .orElseThrow(() -> new AppException(ErrorCode.MATCH_RESULT_NOT_FOUND));
    assertStoredIdsMatchApplication(stored, application);
    return toResponse(stored);
  }

  private ApplicationClientResponse fetchAndValidateAuthorizedApplication(
      UUID applicationId, String accountId, String accountRole) {
    ApplicationClientResponse application;
    try {
      application =
          recruitmentServiceClient.fetchRecruiterApplication(applicationId, accountId, accountRole);
    } catch (DownstreamServiceException exception) {
      throw mapRecruitmentDownstreamError(exception);
    } catch (RuntimeException exception) {
      throw new AppException(ErrorCode.DEPENDENCY_UNAVAILABLE, exception);
    }

    validateAuthorizedApplicationShape(applicationId, application);
    return application;
  }

  private JobMatchingCriteria fetchOwnedJobMatchingCriteria(
      UUID jobId, String accountId, String accountRole) {
    try {
      return jobServiceClient.fetchOwnedJobMatchingCriteria(jobId, accountId, accountRole);
    } catch (DownstreamServiceException exception) {
      throw mapJobDownstreamError(exception);
    } catch (RuntimeException exception) {
      throw new AppException(ErrorCode.DEPENDENCY_UNAVAILABLE, exception);
    }
  }

  private StructuredResume analyzeResume(UUID resumeId) {
    try {
      return resumeAnalysisService.analyzeAndPersist(resumeId);
    } catch (RuntimeException exception) {
      throw new AppException(ErrorCode.DEPENDENCY_UNAVAILABLE, exception);
    }
  }

  private MatchScoreResult score(StructuredResume structuredResume, JobMatchingCriteria criteria) {
    try {
      return deterministicMatchScorer.score(structuredResume, criteria);
    } catch (RuntimeException exception) {
      throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, exception);
    }
  }

  private MatchExplanation generateExplanation(
      JobMatchingCriteria criteria,
      StructuredResume structuredResume,
      MatchScoreResult scoreResult) {
    try {
      return matchExplanationGenerator.generate(criteria, structuredResume, scoreResult);
    } catch (RuntimeException exception) {
      throw new AppException(ErrorCode.DEPENDENCY_UNAVAILABLE, exception);
    }
  }

  private void validateRecruiterRequest(UUID applicationId, String accountId, String accountRole) {
    if (applicationId == null) {
      throw new AppException(ErrorCode.INVALID_REQUEST);
    }
    if (accountId == null || accountId.isBlank()) {
      throw new AppException(ErrorCode.UNAUTHENTICATED);
    }
    if (accountRole == null || accountRole.isBlank()) {
      throw new AppException(ErrorCode.UNAUTHENTICATED);
    }
    if (!RECRUITER_ROLE.equals(accountRole)) {
      throw new AppException(ErrorCode.FORBIDDEN);
    }
  }

  private void validateAuthorizedApplicationShape(
      UUID requestedApplicationId, ApplicationClientResponse application) {
    if (application == null
        || application.getId() == null
        || application.getCandidateId() == null
        || application.getJobId() == null
        || application.getResumeId() == null) {
      throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
    }
    if (!requestedApplicationId.equals(application.getId())) {
      throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
    }
  }

  private void assertStoredIdsMatchApplication(
      MatchResult stored, ApplicationClientResponse application) {
    if (stored.getCandidateId() == null
        || stored.getJobId() == null
        || stored.getResumeId() == null
        || !stored.getCandidateId().equals(application.getCandidateId())
        || !stored.getJobId().equals(application.getJobId())
        || !stored.getResumeId().equals(application.getResumeId())) {
      throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
    }
  }

  private AppException mapRecruitmentDownstreamError(DownstreamServiceException exception) {
    int status = exception.getHttpStatus();
    if (status == 404) {
      return new AppException(ErrorCode.APPLICATION_NOT_FOUND, exception);
    }
    if (status == 401) {
      return new AppException(ErrorCode.UNAUTHENTICATED, exception);
    }
    if (status == 403) {
      return new AppException(ErrorCode.FORBIDDEN, exception);
    }
    return new AppException(ErrorCode.DEPENDENCY_UNAVAILABLE, exception);
  }

  private AppException mapJobDownstreamError(DownstreamServiceException exception) {
    int status = exception.getHttpStatus();
    if (status == 401) {
      return new AppException(ErrorCode.UNAUTHENTICATED, exception);
    }
    if (status == 403) {
      return new AppException(ErrorCode.FORBIDDEN, exception);
    }
    return new AppException(ErrorCode.DEPENDENCY_UNAVAILABLE, exception);
  }

  private MatchResultResponse toResponse(MatchResult matchResult) {
    return new MatchResultResponse(
        matchResult.getApplicationId(),
        matchResult.getCandidateId(),
        matchResult.getJobId(),
        matchResult.getResumeId(),
        matchResult.getTotalScore(),
        matchResult.getSkillsScore(),
        matchResult.getExperienceScore(),
        matchResult.getEducationScore(),
        matchResult.getTitleScore(),
        matchResult.getDomainScore(),
        matchResult.getMatchedSkills(),
        matchResult.getMissingSkills(),
        matchResult.getScoringVersion(),
        matchResult.getScoredAt(),
        matchResult.getExplanation());
  }
}
