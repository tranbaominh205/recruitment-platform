package com.tbm.recruitment.matching.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public record MatchExplanation(
    String summary, List<String> strengths, List<String> gaps, String model, Instant generatedAt) {

  public MatchExplanation {
    summary = normalizeSummary(summary);
    strengths = normalizeList(strengths, 5);
    gaps = normalizeList(gaps, 5);
    model = normalizeModel(model);
    generatedAt = generatedAt == null ? Instant.now() : generatedAt;
  }

  private static String normalizeSummary(String summary) {
    if (summary == null) {
      throw new IllegalArgumentException("Match explanation summary is required");
    }
    String normalized = summary.replaceAll("\\s+", " ").trim();
    if (normalized.isEmpty()) {
      throw new IllegalArgumentException("Match explanation summary is required");
    }
    return normalized;
  }

  private static String normalizeModel(String model) {
    if (model == null) {
      return "";
    }
    String normalized = model.trim();
    return normalized.isEmpty() ? "" : normalized;
  }

  private static List<String> normalizeList(List<String> values, int maxEntries) {
    if (values == null) {
      return List.of();
    }

    List<String> normalized = new ArrayList<>();
    Set<String> seen = new LinkedHashSet<>();
    for (String value : values) {
      if (value == null) {
        continue;
      }
      String trimmed = value.trim();
      if (trimmed.isEmpty()) {
        continue;
      }
      if (seen.add(trimmed)) {
        normalized.add(trimmed);
      }
      if (normalized.size() >= maxEntries) {
        break;
      }
    }
    return List.copyOf(normalized);
  }
}
