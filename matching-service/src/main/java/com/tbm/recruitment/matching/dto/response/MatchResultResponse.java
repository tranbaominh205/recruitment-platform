package com.tbm.recruitment.matching.dto.response;

import com.tbm.recruitment.matching.model.MatchExplanation;
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
    Instant scoredAt,
    MatchExplanation explanation) {

  public MatchResultResponse(
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
    this(
        applicationId,
        candidateId,
        jobId,
        resumeId,
        totalScore,
        skillsScore,
        experienceScore,
        educationScore,
        titleScore,
        domainScore,
        matchedSkills,
        missingSkills,
        scoringVersion,
        scoredAt,
        null);
  }

  public MatchResultResponse {
    matchedSkills = copyList(matchedSkills);
    missingSkills = copyList(missingSkills);
  }

  private static List<String> copyList(List<String> values) {
    return values == null ? List.of() : List.copyOf(values);
  }
}
