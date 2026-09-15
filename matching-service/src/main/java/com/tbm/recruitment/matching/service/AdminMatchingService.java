package com.tbm.recruitment.matching.service;

import com.tbm.recruitment.matching.document.MatchResult;
import com.tbm.recruitment.matching.dto.response.AdminMatchingStatisticsResponse;
import com.tbm.recruitment.matching.dto.response.MatchResultResponse;
import com.tbm.recruitment.matching.dto.response.PageResponse;
import com.tbm.recruitment.matching.exception.AppException;
import com.tbm.recruitment.matching.exception.ErrorCode;
import com.tbm.recruitment.matching.repository.MatchResultRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AdminMatchingService {

  private final MatchResultRepository matchResultRepository;
  private final MongoTemplate mongoTemplate;

  public PageResponse<MatchResultResponse> getResults(
      String accountIdHeader,
      String accountRole,
      UUID jobId,
      UUID candidateId,
      UUID resumeId,
      int page,
      int size) {
    requireAdminAccount(accountIdHeader, accountRole);
    validatePagination(page, size);

    Query query = new Query();
    if (jobId != null) {
      query.addCriteria(Criteria.where("jobId").is(jobId));
    }
    if (candidateId != null) {
      query.addCriteria(Criteria.where("candidateId").is(candidateId));
    }
    if (resumeId != null) {
      query.addCriteria(Criteria.where("resumeId").is(resumeId));
    }

    long totalElements = mongoTemplate.count(query, MatchResult.class);
    query.with(PageRequest.of(page, size)).with(Sort.by(Sort.Direction.DESC, "scoredAt"));

    List<MatchResultResponse> content =
        mongoTemplate.find(query, MatchResult.class).stream().map(this::toResponse).toList();
    int totalPages = (int) Math.ceil((double) totalElements / size);

    return new PageResponse<>(content, page, size, totalElements, totalPages);
  }

  public MatchResultResponse getResultByApplicationId(
      UUID applicationId, String accountIdHeader, String accountRole) {
    requireAdminAccount(accountIdHeader, accountRole);
    MatchResult result =
        matchResultRepository
            .findById(applicationId)
            .orElseThrow(() -> new AppException(ErrorCode.MATCH_RESULT_NOT_FOUND));
    return toResponse(result);
  }

  public AdminMatchingStatisticsResponse getStatistics(String accountIdHeader, String accountRole) {
    requireAdminAccount(accountIdHeader, accountRole);
    return new AdminMatchingStatisticsResponse(matchResultRepository.count());
  }

  private void requireAdminAccount(String accountIdHeader, String accountRole) {
    if (!StringUtils.hasText(accountIdHeader)) {
      throw new AppException(ErrorCode.UNAUTHENTICATED);
    }
    try {
      UUID.fromString(accountIdHeader.trim());
    } catch (IllegalArgumentException exception) {
      throw new AppException(ErrorCode.UNAUTHENTICATED);
    }
    if (!"ADMIN".equals(accountRole)) {
      throw new AppException(ErrorCode.FORBIDDEN);
    }
  }

  private void validatePagination(int page, int size) {
    if (page < 0 || size < 1 || size > 100) {
      throw new AppException(ErrorCode.INVALID_REQUEST);
    }
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
