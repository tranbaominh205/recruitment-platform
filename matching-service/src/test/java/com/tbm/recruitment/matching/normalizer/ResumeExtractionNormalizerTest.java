package com.tbm.recruitment.matching.normalizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tbm.recruitment.matching.model.EducationLevel;
import com.tbm.recruitment.matching.model.ResumeExtractionResult;
import java.util.List;
import org.junit.jupiter.api.Test;

class ResumeExtractionNormalizerTest {

  private final ResumeExtractionNormalizer normalizer = new ResumeExtractionNormalizer();

  @Test
  void trimsAndDeduplicatesValues() {
    ResumeExtractionResult input =
        new ResumeExtractionResult(
            List.of(" Java ", "java", "Spring Boot", "  spring boot  ", ""),
            6.5,
            EducationLevel.BACHELOR,
            List.of(" Backend Developer ", "backend developer", ""),
            List.of(" Software ", "software", " software "));

    ResumeExtractionResult result = normalizer.normalize(input);

    assertEquals(List.of("Java", "Spring Boot"), result.skills());
    assertEquals(List.of("Backend Developer"), result.jobTitles());
    assertEquals(List.of("Software"), result.domains());
  }

  @Test
  void rejectsNegativeExperience() {
    ResumeExtractionResult input =
        new ResumeExtractionResult(
            List.of("Java"), -1.0, EducationLevel.BACHELOR, List.of("Developer"), List.of("Tech"));

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> normalizer.normalize(input));

    assertTrue(exception.getMessage().contains("out of range"));
  }

  @Test
  void rejectsExperienceAboveThreshold() {
    ResumeExtractionResult input =
        new ResumeExtractionResult(
            List.of("Java"), 61.0, EducationLevel.BACHELOR, List.of("Developer"), List.of("Tech"));

    assertThrows(IllegalArgumentException.class, () -> normalizer.normalize(input));
  }

  @Test
  void rejectsNullEducation() {
    ResumeExtractionResult input =
        new ResumeExtractionResult(
            List.of("Java"), 4.0, null, List.of("Developer"), List.of("Tech"));

    assertThrows(IllegalArgumentException.class, () -> normalizer.normalize(input));
  }

  @Test
  void preservesUnknownEducation() {
    ResumeExtractionResult input =
        new ResumeExtractionResult(
            List.of("Java"), 3.0, EducationLevel.UNKNOWN, List.of("Developer"), List.of("Tech"));

    ResumeExtractionResult result = normalizer.normalize(input);

    assertEquals(EducationLevel.UNKNOWN, result.highestEducationLevel());
  }
}
