package com.tbm.recruitment.matching.service;

import com.tbm.recruitment.matching.document.StructuredResume;
import com.tbm.recruitment.matching.model.EducationLevel;
import com.tbm.recruitment.matching.model.JobMatchingCriteria;
import com.tbm.recruitment.matching.model.MatchScoreResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class DeterministicMatchScorer {

  private static final double SKILLS_WEIGHT = 55.0;
  private static final double EXPERIENCE_WEIGHT = 25.0;
  private static final double EDUCATION_WEIGHT = 10.0;
  private static final double TITLE_WEIGHT = 5.0;
  private static final double DOMAIN_WEIGHT = 5.0;
  private static final Pattern REPEATED_WHITESPACE = Pattern.compile("\\s+");
  private static final Pattern NON_ALNUM_RUN = Pattern.compile("[^\\p{L}\\p{Nd}]+");

  public MatchScoreResult score(StructuredResume resume, JobMatchingCriteria job) {
    validateInputs(resume, job);

    SkillsBreakdown skillsBreakdown =
        buildSkillsBreakdown(resume.getSkills(), job.requiredSkills());
    double skillsScore =
        roundHalfUpTwo(
            ((double) skillsBreakdown.matchedSkills().size()
                    / (double) skillsBreakdown.totalUniqueRequiredSkillCount())
                * SKILLS_WEIGHT);

    double experienceScore =
        roundHalfUpTwo(
            scoreExperience(resume.getTotalYearsExperience(), job.minimumYearsExperience()));
    double educationScore =
        roundHalfUpTwo(
            scoreEducation(resume.getHighestEducationLevel(), job.requiredEducationLevel()));
    double titleScore =
        roundHalfUpTwo(scoreTextSimilarity(job.title(), resume.getJobTitles(), TITLE_WEIGHT));
    double domainScore =
        roundHalfUpTwo(scoreTextSimilarity(job.domain(), resume.getDomains(), DOMAIN_WEIGHT));

    double totalScore =
        roundHalfUpTwo(skillsScore + experienceScore + educationScore + titleScore + domainScore);
    if (totalScore < 0.0 || totalScore > 100.0) {
      throw new IllegalStateException("Total score is out of range");
    }

    return new MatchScoreResult(
        totalScore,
        skillsScore,
        experienceScore,
        educationScore,
        titleScore,
        domainScore,
        skillsBreakdown.matchedSkills(),
        skillsBreakdown.missingSkills());
  }

  private void validateInputs(StructuredResume resume, JobMatchingCriteria job) {
    if (resume == null) {
      throw new IllegalArgumentException("StructuredResume is required");
    }
    if (job == null) {
      throw new IllegalArgumentException("JobMatchingCriteria is required");
    }

    if (isBlank(job.title())) {
      throw new IllegalArgumentException("Job title is required");
    }
    if (isBlank(job.domain())) {
      throw new IllegalArgumentException("Job domain is required");
    }
    if (job.requiredSkills() == null || job.requiredSkills().isEmpty()) {
      throw new IllegalArgumentException("Job required skills are required");
    }
    if (job.minimumYearsExperience() < 0 || job.minimumYearsExperience() > 50) {
      throw new IllegalArgumentException("Job minimum years experience is out of range");
    }
    if (job.requiredEducationLevel() == null) {
      throw new IllegalArgumentException("Job required education level is required");
    }
    if (job.requiredEducationLevel() == EducationLevel.UNKNOWN) {
      throw new IllegalArgumentException("Job required education level cannot be UNKNOWN");
    }

    if (Double.isNaN(resume.getTotalYearsExperience())
        || Double.isInfinite(resume.getTotalYearsExperience())
        || resume.getTotalYearsExperience() < 0.0
        || resume.getTotalYearsExperience() > 60.0) {
      throw new IllegalArgumentException("Candidate total years experience is out of range");
    }
    if (resume.getHighestEducationLevel() == null) {
      throw new IllegalArgumentException("Candidate highest education level is required");
    }
  }

  private SkillsBreakdown buildSkillsBreakdown(
      List<String> candidateSkillsInput, List<String> requiredSkillsInput) {
    Map<String, String> requiredByKey = new LinkedHashMap<>();
    for (String requiredSkill : requiredSkillsInput) {
      String displayValue = normalizeDisplaySkill(requiredSkill);
      if (displayValue.isEmpty()) {
        throw new IllegalArgumentException("Job required skill cannot be blank");
      }
      String key = normalizeSkillKey(requiredSkill);
      requiredByKey.putIfAbsent(key, displayValue);
    }

    Set<String> candidateKeys = new LinkedHashSet<>();
    List<String> candidateSkills = candidateSkillsInput == null ? List.of() : candidateSkillsInput;
    for (String candidateSkill : candidateSkills) {
      String key = normalizeSkillKey(candidateSkill);
      if (!key.isEmpty()) {
        candidateKeys.add(key);
      }
    }

    List<String> matchedSkills = new ArrayList<>();
    List<String> missingSkills = new ArrayList<>();
    for (Map.Entry<String, String> required : requiredByKey.entrySet()) {
      if (candidateKeys.contains(required.getKey())) {
        matchedSkills.add(required.getValue());
      } else {
        missingSkills.add(required.getValue());
      }
    }

    return new SkillsBreakdown(
        requiredByKey.size(), List.copyOf(matchedSkills), List.copyOf(missingSkills));
  }

  private double scoreExperience(double candidateYears, int requiredYears) {
    if (requiredYears == 0) {
      return EXPERIENCE_WEIGHT;
    }
    return Math.min(candidateYears / requiredYears, 1.0) * EXPERIENCE_WEIGHT;
  }

  private double scoreEducation(EducationLevel candidate, EducationLevel required) {
    if (required == EducationLevel.NONE) {
      return EDUCATION_WEIGHT;
    }
    if (candidate == EducationLevel.UNKNOWN) {
      return 0.0;
    }
    return educationRank(candidate) >= educationRank(required) ? EDUCATION_WEIGHT : 0.0;
  }

  private int educationRank(EducationLevel level) {
    return switch (level) {
      case NONE -> 0;
      case HIGH_SCHOOL -> 1;
      case DIPLOMA -> 2;
      case ASSOCIATE -> 3;
      case BACHELOR -> 4;
      case MASTER -> 5;
      case DOCTORATE -> 6;
      case UNKNOWN -> -1;
    };
  }

  private double scoreTextSimilarity(String jobText, List<String> candidateTexts, double weight) {
    List<String> candidates = candidateTexts == null ? List.of() : candidateTexts;
    if (candidates.isEmpty()) {
      return 0.0;
    }

    Set<String> jobTokens = tokenizeForJaccard(jobText);
    double maxSimilarity = 0.0;
    for (String candidateText : candidates) {
      Set<String> candidateTokens = tokenizeForJaccard(candidateText);
      double similarity = jaccard(jobTokens, candidateTokens);
      if (similarity > maxSimilarity) {
        maxSimilarity = similarity;
      }
    }
    return maxSimilarity * weight;
  }

  private Set<String> tokenizeForJaccard(String input) {
    String normalized = normalizeForTextSimilarity(input);
    if (normalized.isEmpty()) {
      return Set.of();
    }

    Set<String> tokens = new LinkedHashSet<>();
    for (String token : normalized.split(" ")) {
      if (!token.isEmpty()) {
        tokens.add(token);
      }
    }
    return tokens;
  }

  private double jaccard(Set<String> left, Set<String> right) {
    if (left.isEmpty() && right.isEmpty()) {
      return 0.0;
    }

    Set<String> intersection = new LinkedHashSet<>(left);
    intersection.retainAll(right);

    Set<String> union = new LinkedHashSet<>(left);
    union.addAll(right);

    if (union.isEmpty()) {
      return 0.0;
    }
    return (double) intersection.size() / (double) union.size();
  }

  private String normalizeSkillKey(String skill) {
    return normalizeDisplaySkill(skill).toLowerCase(Locale.ROOT);
  }

  private String normalizeDisplaySkill(String skill) {
    if (skill == null) {
      return "";
    }
    String normalized = Normalizer.normalize(skill, Normalizer.Form.NFKC).trim();
    return collapseWhitespace(normalized);
  }

  private String normalizeForTextSimilarity(String value) {
    if (value == null) {
      return "";
    }
    String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
    String replaced = NON_ALNUM_RUN.matcher(normalized).replaceAll(" ");
    return collapseWhitespace(replaced);
  }

  private String collapseWhitespace(String value) {
    return REPEATED_WHITESPACE.matcher(value).replaceAll(" ").trim();
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private double roundHalfUpTwo(double value) {
    return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
  }

  private record SkillsBreakdown(
      int totalUniqueRequiredSkillCount, List<String> matchedSkills, List<String> missingSkills) {}
}
