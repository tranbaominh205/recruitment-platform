package com.tbm.recruitment.matching.service;

import com.tbm.recruitment.matching.document.StructuredResume;
import com.tbm.recruitment.matching.extractor.ResumeStructuredExtractor;
import com.tbm.recruitment.matching.model.ResumeExtractionResult;
import com.tbm.recruitment.matching.normalizer.ResumeExtractionNormalizer;
import com.tbm.recruitment.matching.repository.StructuredResumeRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResumeAnalysisService {

  private final StructuredResumeRepository structuredResumeRepository;
  private final ResumeParsingService resumeParsingService;
  private final ResumeStructuredExtractor resumeStructuredExtractor;
  private final ResumeExtractionNormalizer resumeExtractionNormalizer;

  @Value("${spring.ai.google.genai.chat.model:gemini-3.5-flash}")
  private String extractionModel;

  public StructuredResume analyzeAndPersist(UUID resumeId) {
    if (resumeId == null) {
      throw new IllegalArgumentException("resumeId is required");
    }

    return structuredResumeRepository
        .findById(resumeId)
        .orElseGet(
            () -> {
              String rawText = resumeParsingService.parseAndPersist(resumeId).getRawText();
              if (rawText == null || rawText.isBlank()) {
                throw new IllegalStateException("Resume raw text is blank");
              }

              ResumeExtractionResult extractionResult = resumeStructuredExtractor.extract(rawText);
              ResumeExtractionResult normalizedResult =
                  resumeExtractionNormalizer.normalize(extractionResult);

              StructuredResume structuredResume =
                  StructuredResume.builder()
                      .resumeId(resumeId)
                      .skills(normalizedResult.skills())
                      .totalYearsExperience(normalizedResult.totalYearsExperience())
                      .highestEducationLevel(normalizedResult.highestEducationLevel())
                      .jobTitles(normalizedResult.jobTitles())
                      .domains(normalizedResult.domains())
                      .analyzedAt(Instant.now())
                      .extractionModel(extractionModel)
                      .build();

              return structuredResumeRepository.save(structuredResume);
            });
  }
}
