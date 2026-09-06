package com.tbm.recruitment.matching.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MatchResultResponse(
    UUID applicationId,
    UUID candidateId,
    UUID jobId,
    UUID resumeId,
    double totalScore,
    double skillsScore,
    double experienceScore,
    double educationScore,
    double titleScore,
    double domainScore,
    List<String> matchedSkills,
    List<String> missingSkills,
    String scoringVersion,
    Instant scoredAt) {

  public MatchResultResponse {
    matchedSkills = copyList(matchedSkills);
    missingSkills = copyList(missingSkills);
  }

  private static List<String> copyList(List<String> values) {
    return values == null ? List.of() : List.copyOf(values);
  }
}
