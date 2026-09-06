package com.tbm.recruitment.matching.normalizer;

import com.tbm.recruitment.matching.model.ResumeExtractionResult;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class ResumeExtractionNormalizer {

  public ResumeExtractionResult normalize(ResumeExtractionResult input) {
    if (input == null) {
      throw new IllegalArgumentException("Resume extraction result is required");
    }

    if (Double.isNaN(input.totalYearsExperience())
        || Double.isInfinite(input.totalYearsExperience())
        || input.totalYearsExperience() < 0.0
        || input.totalYearsExperience() > 60.0) {
      throw new IllegalArgumentException("Total years experience is out of range");
    }

    if (input.highestEducationLevel() == null) {
      throw new IllegalArgumentException("Highest education level is required");
    }

    List<String> skills = normalizeValues(input.skills(), 100);
    List<String> jobTitles = normalizeValues(input.jobTitles(), 30);
    List<String> domains = normalizeValues(input.domains(), 30);

    return new ResumeExtractionResult(
        skills, input.totalYearsExperience(), input.highestEducationLevel(), jobTitles, domains);
  }

  private List<String> normalizeValues(List<String> values, int maxItems) {
    if (values == null) {
      return new ArrayList<>();
    }

    Set<String> seen = new LinkedHashSet<>();
    List<String> normalized = new ArrayList<>();

    for (String value : values) {
      if (value == null) {
        continue;
      }

      String trimmed = value.trim();
      if (trimmed.isEmpty()) {
        continue;
      }

      String canonical = trimmed.replaceAll("\\s+", " ");
      String key = canonical.toLowerCase(Locale.ROOT);
      if (seen.add(key)) {
        normalized.add(canonical);
      }
    }

    if (normalized.size() > maxItems) {
      throw new IllegalArgumentException("Too many values for a normalized resume field");
    }

    return normalized;
  }
}
