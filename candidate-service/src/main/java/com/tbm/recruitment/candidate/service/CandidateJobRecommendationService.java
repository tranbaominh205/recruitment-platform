package com.tbm.recruitment.candidate.service;

import com.tbm.recruitment.candidate.client.JobClient;
import com.tbm.recruitment.candidate.client.dto.JobSearchJobResponse;
import com.tbm.recruitment.candidate.dto.response.PageResponse;
import com.tbm.recruitment.candidate.dto.response.RecommendedJobResponse;
import com.tbm.recruitment.candidate.entity.CandidateProfile;
import com.tbm.recruitment.candidate.exception.AppException;
import com.tbm.recruitment.candidate.exception.ErrorCode;
import com.tbm.recruitment.candidate.repository.CandidateProfileRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CandidateJobRecommendationService {

  static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

  static final String REASON_TITLE_MATCH = "TITLE_MATCH";
  static final String REASON_LOCATION_MATCH = "LOCATION_MATCH";
  static final String REASON_EMPLOYMENT_TYPE_MATCH = "EMPLOYMENT_TYPE_MATCH";
  static final String REASON_WORKPLACE_TYPE_MATCH = "WORKPLACE_TYPE_MATCH";

  static final int TITLE_WEIGHT = 40;
  static final int LOCATION_WEIGHT = 25;
  static final int EMPLOYMENT_TYPE_WEIGHT = 20;
  static final int WORKPLACE_TYPE_WEIGHT = 15;

  static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
  static final BigDecimal ZERO_SCORE = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

  CandidateProfileRepository candidateProfileRepository;
  JobClient jobClient;

  @Transactional(readOnly = true)
  public PageResponse<RecommendedJobResponse> getRecommendations(
      String accountIdHeader, String accountRole, int page, int size) {
    validatePagination(page, size);
    UUID accountId = requireCandidateAccount(accountIdHeader, accountRole);

    CandidateProfile candidateProfile =
        candidateProfileRepository
            .findByAccountId(accountId)
            .orElseThrow(() -> new AppException(ErrorCode.PROFILE_NOT_FOUND));

    Set<String> desiredTitles = normalizeSet(candidateProfile.getDesiredJobTitles());
    Set<String> preferredLocations = normalizeSet(candidateProfile.getPreferredLocations());
    Set<String> employmentTypes = normalizeSet(candidateProfile.getEmploymentTypes());
    Set<String> workplaceTypes = normalizeSet(candidateProfile.getWorkplaceTypes());

    boolean hasPreferences =
        !desiredTitles.isEmpty()
            || !preferredLocations.isEmpty()
            || !employmentTypes.isEmpty()
            || !workplaceTypes.isEmpty();

    List<RecommendedJobResponse> sortedRecommendations =
        jobClient.fetchAllJobsForRecommendation().stream()
            .filter(this::isEligibleForRecommendation)
            .map(
                job ->
                    toRecommendedJob(
                        job,
                        desiredTitles,
                        preferredLocations,
                        employmentTypes,
                        workplaceTypes,
                        hasPreferences))
            .sorted(recommendationComparator())
            .toList();

    return paginate(sortedRecommendations, page, size);
  }

  private Comparator<RecommendedJobResponse> recommendationComparator() {
    return Comparator.comparing(RecommendedJobResponse::recommendationScore)
        .reversed()
        .thenComparing(
            RecommendedJobResponse::createdAt, Comparator.nullsLast(Comparator.reverseOrder()))
        .thenComparing(RecommendedJobResponse::id, Comparator.nullsLast(Comparator.naturalOrder()));
  }

  private RecommendedJobResponse toRecommendedJob(
      JobSearchJobResponse job,
      Set<String> desiredTitles,
      Set<String> preferredLocations,
      Set<String> employmentTypes,
      Set<String> workplaceTypes,
      boolean hasPreferences) {

    if (!hasPreferences) {
      return new RecommendedJobResponse(
          job.id(),
          job.companyId(),
          job.title(),
          job.description(),
          job.location(),
          job.employmentType(),
          job.workplaceType(),
          job.createdAt(),
          ZERO_SCORE,
          List.of());
    }

    String normalizedTitle = normalize(job.title());
    String normalizedLocation = normalize(job.location());
    String normalizedEmploymentType = normalize(job.employmentType());
    String normalizedWorkplaceType = normalize(job.workplaceType());

    int totalActiveWeight = 0;
    int matchedActiveWeight = 0;
    List<String> reasons = new ArrayList<>();

    if (!desiredTitles.isEmpty()) {
      totalActiveWeight += TITLE_WEIGHT;
      if (matchesTextByExactOrContainment(desiredTitles, normalizedTitle)) {
        matchedActiveWeight += TITLE_WEIGHT;
        reasons.add(REASON_TITLE_MATCH);
      }
    }

    if (!preferredLocations.isEmpty()) {
      totalActiveWeight += LOCATION_WEIGHT;
      if (matchesTextByExactOrContainment(preferredLocations, normalizedLocation)) {
        matchedActiveWeight += LOCATION_WEIGHT;
        reasons.add(REASON_LOCATION_MATCH);
      }
    }

    if (!employmentTypes.isEmpty()) {
      totalActiveWeight += EMPLOYMENT_TYPE_WEIGHT;
      if (employmentTypes.contains(normalizedEmploymentType)) {
        matchedActiveWeight += EMPLOYMENT_TYPE_WEIGHT;
        reasons.add(REASON_EMPLOYMENT_TYPE_MATCH);
      }
    }

    if (!workplaceTypes.isEmpty()) {
      totalActiveWeight += WORKPLACE_TYPE_WEIGHT;
      if (workplaceTypes.contains(normalizedWorkplaceType)) {
        matchedActiveWeight += WORKPLACE_TYPE_WEIGHT;
        reasons.add(REASON_WORKPLACE_TYPE_MATCH);
      }
    }

    BigDecimal recommendationScore = calculateScore(matchedActiveWeight, totalActiveWeight);

    return new RecommendedJobResponse(
        job.id(),
        job.companyId(),
        job.title(),
        job.description(),
        job.location(),
        job.employmentType(),
        job.workplaceType(),
        job.createdAt(),
        recommendationScore,
        List.copyOf(reasons));
  }

  private BigDecimal calculateScore(int matchedWeight, int totalWeight) {
    if (matchedWeight <= 0 || totalWeight <= 0) {
      return ZERO_SCORE;
    }

    return BigDecimal.valueOf(matchedWeight)
        .multiply(HUNDRED)
        .divide(BigDecimal.valueOf(totalWeight), 2, RoundingMode.HALF_UP);
  }

  private boolean matchesTextByExactOrContainment(Set<String> preferences, String jobValue) {
    if (jobValue.isEmpty()) {
      return false;
    }

    for (String preference : preferences) {
      if (preference.equals(jobValue)
          || preference.contains(jobValue)
          || jobValue.contains(preference)) {
        return true;
      }
    }

    return false;
  }

  private boolean isEligibleForRecommendation(JobSearchJobResponse job) {
    return "PUBLISHED".equals(job.status()) && "ACTIVE".equals(job.moderationStatus());
  }

  private PageResponse<RecommendedJobResponse> paginate(
      List<RecommendedJobResponse> recommendations, int page, int size) {
    long totalElements = recommendations.size();
    int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
    int fromIndex = page * size;
    if (fromIndex >= recommendations.size()) {
      return new PageResponse<>(List.of(), page, size, totalElements, totalPages);
    }

    int toIndex = Math.min(fromIndex + size, recommendations.size());
    return new PageResponse<>(
        recommendations.subList(fromIndex, toIndex), page, size, totalElements, totalPages);
  }

  private Set<String> normalizeSet(Set<String> values) {
    if (values == null || values.isEmpty()) {
      return Set.of();
    }

    LinkedHashSet<String> normalized = new LinkedHashSet<>();
    for (String value : values) {
      String normalizedValue = normalize(value);
      if (!normalizedValue.isEmpty()) {
        normalized.add(normalizedValue);
      }
    }
    return normalized;
  }

  private String normalize(String input) {
    if (input == null) {
      return "";
    }

    String trimmedLowercase = input.trim().toLowerCase(Locale.ROOT);
    if (trimmedLowercase.isEmpty()) {
      return "";
    }

    return WHITESPACE_PATTERN.matcher(trimmedLowercase).replaceAll(" ");
  }

  private void validatePagination(int page, int size) {
    if (page < 0 || size < 1 || size > 100) {
      throw new AppException(ErrorCode.INVALID_REQUEST);
    }
  }

  private UUID requireCandidateAccount(String accountIdHeader, String accountRole) {
    if (accountIdHeader == null || accountIdHeader.isBlank()) {
      throw new AppException(ErrorCode.UNAUTHENTICATED);
    }

    if (!"CANDIDATE".equals(accountRole)) {
      throw new AppException(ErrorCode.FORBIDDEN);
    }

    try {
      return UUID.fromString(accountIdHeader);
    } catch (IllegalArgumentException exception) {
      throw new AppException(ErrorCode.UNAUTHENTICATED);
    }
  }
}
