package com.tbm.recruitment.matching.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tbm.recruitment.matching.document.StructuredResume;
import com.tbm.recruitment.matching.model.EducationLevel;
import com.tbm.recruitment.matching.model.JobMatchingCriteria;
import com.tbm.recruitment.matching.model.MatchScoreResult;
import java.util.List;
import org.junit.jupiter.api.Test;

class DeterministicMatchScorerTest {

  private final DeterministicMatchScorer scorer = new DeterministicMatchScorer();

  @Test
  void perfectMatchReturnsExactly100() {
    StructuredResume resume =
        resume(
            List.of("Java", "Spring Boot"),
            5.0,
            EducationLevel.BACHELOR,
            List.of("Backend Engineer"),
            List.of("Software Engineering"));
    JobMatchingCriteria job =
        job(
            "Backend Engineer",
            List.of("Java", "Spring Boot"),
            5,
            EducationLevel.BACHELOR,
            "Software Engineering");

    MatchScoreResult result = scorer.score(resume, job);

    assertEquals(100.0, result.totalScore(), 0.0001);
    assertEquals(55.0, result.skillsScore(), 0.0001);
    assertEquals(25.0, result.experienceScore(), 0.0001);
    assertEquals(10.0, result.educationScore(), 0.0001);
    assertEquals(5.0, result.titleScore(), 0.0001);
    assertEquals(5.0, result.domainScore(), 0.0001);
  }

  @Test
  void partialSkillMatchingUses55WeightCorrectly() {
    StructuredResume resume =
        resume(
            List.of("Java", "Spring Boot"), 0.0, EducationLevel.UNKNOWN, List.of(""), List.of(""));
    JobMatchingCriteria job =
        job(
            "Any Title",
            List.of("Java", "Spring Boot", "Docker", "Kubernetes"),
            10,
            EducationLevel.BACHELOR,
            "Any Domain");

    MatchScoreResult result = scorer.score(resume, job);

    assertEquals(27.5, result.skillsScore(), 0.0001);
  }

  @Test
  void extraCandidateSkillsDoNotProduceBonus() {
    StructuredResume resume =
        resume(
            List.of("Java", "Spring Boot", "Docker", "Redis"),
            0.0,
            EducationLevel.UNKNOWN,
            List.of(""),
            List.of(""));
    JobMatchingCriteria job =
        job("Any Title", List.of("Java", "Spring Boot"), 10, EducationLevel.BACHELOR, "Any Domain");

    MatchScoreResult result = scorer.score(resume, job);

    assertEquals(55.0, result.skillsScore(), 0.0001);
    assertEquals(List.of("Java", "Spring Boot"), result.matchedSkills());
    assertEquals(List.of(), result.missingSkills());
  }

  @Test
  void skillComparisonIsCaseAndWhitespaceInsensitive() {
    StructuredResume resume =
        resume(
            List.of("  spring   boot ", "JAVA"),
            0.0,
            EducationLevel.UNKNOWN,
            List.of(""),
            List.of(""));
    JobMatchingCriteria job =
        job("Any Title", List.of("Spring Boot", "java"), 10, EducationLevel.BACHELOR, "Any Domain");

    MatchScoreResult result = scorer.score(resume, job);

    assertEquals(55.0, result.skillsScore(), 0.0001);
    assertEquals(List.of("Spring Boot", "java"), result.matchedSkills());
  }

  @Test
  void cPlusPlusAndCSharpRemainDistinct() {
    StructuredResume resume =
        resume(List.of("C++"), 0.0, EducationLevel.UNKNOWN, List.of(""), List.of(""));
    JobMatchingCriteria job =
        job("Any Title", List.of("C#"), 10, EducationLevel.BACHELOR, "Any Domain");

    MatchScoreResult result = scorer.score(resume, job);

    assertEquals(0.0, result.skillsScore(), 0.0001);
    assertEquals(List.of(), result.matchedSkills());
    assertEquals(List.of("C#"), result.missingSkills());
  }

  @Test
  void javaAndJavaScriptRemainDistinct() {
    StructuredResume resume =
        resume(List.of("Java"), 0.0, EducationLevel.UNKNOWN, List.of(""), List.of(""));
    JobMatchingCriteria job =
        job("Any Title", List.of("JavaScript"), 10, EducationLevel.BACHELOR, "Any Domain");

    MatchScoreResult result = scorer.score(resume, job);

    assertEquals(0.0, result.skillsScore(), 0.0001);
    assertEquals(List.of("JavaScript"), result.missingSkills());
  }

  @Test
  void partialExperienceIsProportional() {
    StructuredResume resume =
        resume(List.of("Java"), 2.0, EducationLevel.UNKNOWN, List.of(""), List.of(""));
    JobMatchingCriteria job =
        job("Any Title", List.of("Java"), 4, EducationLevel.BACHELOR, "Any Domain");

    MatchScoreResult result = scorer.score(resume, job);

    assertEquals(12.5, result.experienceScore(), 0.0001);
  }

  @Test
  void experienceMeetingOrExceedingRequirementCapsAt25() {
    StructuredResume resume =
        resume(List.of("Java"), 7.0, EducationLevel.UNKNOWN, List.of(""), List.of(""));
    JobMatchingCriteria job =
        job("Any Title", List.of("Java"), 5, EducationLevel.BACHELOR, "Any Domain");

    MatchScoreResult result = scorer.score(resume, job);

    assertEquals(25.0, result.experienceScore(), 0.0001);
  }

  @Test
  void zeroRequiredExperienceGives25() {
    StructuredResume resume =
        resume(List.of("Java"), 0.0, EducationLevel.UNKNOWN, List.of(""), List.of(""));
    JobMatchingCriteria job =
        job("Any Title", List.of("Java"), 0, EducationLevel.BACHELOR, "Any Domain");

    MatchScoreResult result = scorer.score(resume, job);

    assertEquals(25.0, result.experienceScore(), 0.0001);
  }

  @Test
  void educationMeetsOrExceedsRequirementGives10() {
    StructuredResume resume =
        resume(List.of("Java"), 0.0, EducationLevel.MASTER, List.of(""), List.of(""));
    JobMatchingCriteria job =
        job("Any Title", List.of("Java"), 10, EducationLevel.BACHELOR, "Any Domain");

    MatchScoreResult result = scorer.score(resume, job);

    assertEquals(10.0, result.educationScore(), 0.0001);
  }

  @Test
  void educationBelowRequirementGives0() {
    StructuredResume resume =
        resume(List.of("Java"), 0.0, EducationLevel.ASSOCIATE, List.of(""), List.of(""));
    JobMatchingCriteria job =
        job("Any Title", List.of("Java"), 10, EducationLevel.BACHELOR, "Any Domain");

    MatchScoreResult result = scorer.score(resume, job);

    assertEquals(0.0, result.educationScore(), 0.0001);
  }

  @Test
  void unknownCandidateEducationGives0WhenEducationIsRequired() {
    StructuredResume resume =
        resume(List.of("Java"), 0.0, EducationLevel.UNKNOWN, List.of(""), List.of(""));
    JobMatchingCriteria job =
        job("Any Title", List.of("Java"), 10, EducationLevel.BACHELOR, "Any Domain");

    MatchScoreResult result = scorer.score(resume, job);

    assertEquals(0.0, result.educationScore(), 0.0001);
  }

  @Test
  void requiredNoneEducationGives10() {
    StructuredResume resume =
        resume(List.of("Java"), 0.0, EducationLevel.UNKNOWN, List.of(""), List.of(""));
    JobMatchingCriteria job =
        job("Any Title", List.of("Java"), 10, EducationLevel.NONE, "Any Domain");

    MatchScoreResult result = scorer.score(resume, job);

    assertEquals(10.0, result.educationScore(), 0.0001);
  }

  @Test
  void exactTitleGives5() {
    StructuredResume resume =
        resume(
            List.of("Java"),
            0.0,
            EducationLevel.UNKNOWN,
            List.of("Senior Backend Engineer"),
            List.of(""));
    JobMatchingCriteria job =
        job("Senior Backend Engineer", List.of("Java"), 10, EducationLevel.BACHELOR, "Any Domain");

    MatchScoreResult result = scorer.score(resume, job);

    assertEquals(5.0, result.titleScore(), 0.0001);
  }

  @Test
  void partialTitleJaccardProducesDeterministicPartialScore() {
    StructuredResume resume =
        resume(
            List.of("Java"), 0.0, EducationLevel.UNKNOWN, List.of("Backend Engineer"), List.of(""));
    JobMatchingCriteria job =
        job("Senior Backend Engineer", List.of("Java"), 10, EducationLevel.BACHELOR, "Any Domain");

    MatchScoreResult result = scorer.score(resume, job);

    assertEquals(3.33, result.titleScore(), 0.0001);
  }

  @Test
  void exactDomainGives5() {
    StructuredResume resume =
        resume(
            List.of("Java"), 0.0, EducationLevel.UNKNOWN, List.of(""), List.of("Data Engineering"));
    JobMatchingCriteria job =
        job("Any Title", List.of("Java"), 10, EducationLevel.BACHELOR, "Data Engineering");

    MatchScoreResult result = scorer.score(resume, job);

    assertEquals(5.0, result.domainScore(), 0.0001);
  }

  @Test
  void missingCandidateTitlesAndDomainsGiveZeroForThoseComponents() {
    StructuredResume resume = resume(List.of("Java"), 0.0, EducationLevel.UNKNOWN, null, null);
    JobMatchingCriteria job =
        job("Any Title", List.of("Java"), 10, EducationLevel.BACHELOR, "Any Domain");

    MatchScoreResult result = scorer.score(resume, job);

    assertEquals(0.0, result.titleScore(), 0.0001);
    assertEquals(0.0, result.domainScore(), 0.0001);
  }

  @Test
  void totalEqualsVisibleComponentSum() {
    StructuredResume resume =
        resume(
            List.of("Java", "Spring Boot", "Docker"),
            1.0,
            EducationLevel.BACHELOR,
            List.of("Backend Engineer"),
            List.of("Software"));
    JobMatchingCriteria job =
        job(
            "Senior Backend Engineer",
            List.of("Java", "Spring Boot", "Kubernetes", "Microservices"),
            3,
            EducationLevel.BACHELOR,
            "Software Engineering");

    MatchScoreResult result = scorer.score(resume, job);

    assertEquals(27.5, result.skillsScore(), 0.0001);
    assertEquals(8.33, result.experienceScore(), 0.0001);
    assertEquals(10.0, result.educationScore(), 0.0001);
    assertEquals(3.33, result.titleScore(), 0.0001);
    assertEquals(2.5, result.domainScore(), 0.0001);
    assertEquals(51.66, result.totalScore(), 0.0001);
    assertEquals(
        result.skillsScore()
            + result.experienceScore()
            + result.educationScore()
            + result.titleScore()
            + result.domainScore(),
        result.totalScore(),
        0.0001);
  }

  @Test
  void invalidJobMatchingCriteriaIsRejected() {
    StructuredResume resume =
        resume(
            List.of("Java"),
            2.0,
            EducationLevel.BACHELOR,
            List.of("Developer"),
            List.of("Software"));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            scorer.score(
                resume,
                new JobMatchingCriteria(
                    " ", List.of("Java"), 1, EducationLevel.BACHELOR, "Software")));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            scorer.score(
                resume,
                new JobMatchingCriteria("Dev", null, 1, EducationLevel.BACHELOR, "Software")));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            scorer.score(
                resume,
                new JobMatchingCriteria(
                    "Dev", List.of("Java"), -1, EducationLevel.BACHELOR, "Software")));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            scorer.score(
                resume,
                new JobMatchingCriteria(
                    "Dev", List.of("Java"), 1, EducationLevel.UNKNOWN, "Software")));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            scorer.score(
                resume,
                new JobMatchingCriteria(
                    "Dev", List.of("Java", "   "), 1, EducationLevel.BACHELOR, "Software")));
  }

  @Test
  void invalidCandidateMatchingDataIsRejected() {
    JobMatchingCriteria job =
        job("Backend Engineer", List.of("Java"), 2, EducationLevel.BACHELOR, "Software");

    assertThrows(IllegalArgumentException.class, () -> scorer.score(null, job));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            scorer.score(
                resume(
                    List.of("Java"),
                    Double.NaN,
                    EducationLevel.BACHELOR,
                    List.of("Dev"),
                    List.of("Software")),
                job));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            scorer.score(
                resume(
                    List.of("Java"),
                    Double.POSITIVE_INFINITY,
                    EducationLevel.BACHELOR,
                    List.of("Dev"),
                    List.of("Software")),
                job));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            scorer.score(
                resume(
                    List.of("Java"),
                    -0.1,
                    EducationLevel.BACHELOR,
                    List.of("Dev"),
                    List.of("Software")),
                job));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            scorer.score(
                resume(
                    List.of("Java"),
                    61.0,
                    EducationLevel.BACHELOR,
                    List.of("Dev"),
                    List.of("Software")),
                job));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            scorer.score(
                resume(List.of("Java"), 2.0, null, List.of("Dev"), List.of("Software")), job));
  }

  private StructuredResume resume(
      List<String> skills,
      double years,
      EducationLevel level,
      List<String> jobTitles,
      List<String> domains) {
    return StructuredResume.builder()
        .skills(skills)
        .totalYearsExperience(years)
        .highestEducationLevel(level)
        .jobTitles(jobTitles)
        .domains(domains)
        .build();
  }

  private JobMatchingCriteria job(
      String title,
      List<String> requiredSkills,
      int years,
      EducationLevel education,
      String domain) {
    return new JobMatchingCriteria(title, requiredSkills, years, education, domain);
  }
}
