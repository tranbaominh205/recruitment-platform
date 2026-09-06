package com.tbm.recruitment.matching.document;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "match_results")
@Getter
@Setter
@Builder
public class MatchResult {

  @Id private UUID applicationId;

  private UUID candidateId;

  private UUID jobId;

  private UUID resumeId;

  private double totalScore;

  private double skillsScore;

  private double experienceScore;

  private double educationScore;

  private double titleScore;

  private double domainScore;

  private List<String> matchedSkills;

  private List<String> missingSkills;

  private String scoringVersion;

  private Instant scoredAt;

  public MatchResult() {
    this.matchedSkills = List.of();
    this.missingSkills = List.of();
  }

  public MatchResult(
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
    this.applicationId = applicationId;
    this.candidateId = candidateId;
    this.jobId = jobId;
    this.resumeId = resumeId;
    this.totalScore = totalScore;
    this.skillsScore = skillsScore;
    this.experienceScore = experienceScore;
    this.educationScore = educationScore;
    this.titleScore = titleScore;
    this.domainScore = domainScore;
    this.matchedSkills = copyList(matchedSkills);
    this.missingSkills = copyList(missingSkills);
    this.scoringVersion = scoringVersion;
    this.scoredAt = scoredAt;
  }

  public void setMatchedSkills(List<String> matchedSkills) {
    this.matchedSkills = copyList(matchedSkills);
  }

  public void setMissingSkills(List<String> missingSkills) {
    this.missingSkills = copyList(missingSkills);
  }

  private static List<String> copyList(List<String> values) {
    return values == null ? List.of() : List.copyOf(values);
  }
}
