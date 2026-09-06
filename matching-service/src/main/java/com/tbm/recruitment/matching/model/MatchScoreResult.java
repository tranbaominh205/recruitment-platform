package com.tbm.recruitment.matching.model;

import java.util.List;

public record MatchScoreResult(
    double totalScore,
    double skillsScore,
    double experienceScore,
    double educationScore,
    double titleScore,
    double domainScore,
    List<String> matchedSkills,
    List<String> missingSkills) {

  public MatchScoreResult {
    matchedSkills = List.copyOf(matchedSkills);
    missingSkills = List.copyOf(missingSkills);
  }
}
