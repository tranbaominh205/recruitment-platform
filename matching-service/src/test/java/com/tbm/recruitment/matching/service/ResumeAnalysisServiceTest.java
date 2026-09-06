package com.tbm.recruitment.matching.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tbm.recruitment.matching.document.ParsedResume;
import com.tbm.recruitment.matching.document.StructuredResume;
import com.tbm.recruitment.matching.extractor.ResumeStructuredExtractor;
import com.tbm.recruitment.matching.model.EducationLevel;
import com.tbm.recruitment.matching.model.ResumeExtractionResult;
import com.tbm.recruitment.matching.normalizer.ResumeExtractionNormalizer;
import com.tbm.recruitment.matching.repository.StructuredResumeRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ResumeAnalysisServiceTest {

  @Mock private StructuredResumeRepository structuredResumeRepository;
  @Mock private ResumeParsingService resumeParsingService;
  @Mock private ResumeStructuredExtractor resumeStructuredExtractor;
  @Mock private ResumeExtractionNormalizer resumeExtractionNormalizer;

  @InjectMocks private ResumeAnalysisService resumeAnalysisService;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(resumeAnalysisService, "extractionModel", "gemini-3.5-flash");
  }

  @Test
  void analysesAndPersistsNewResume() {
    UUID resumeId = UUID.randomUUID();
    ParsedResume parsedResume =
        ParsedResume.builder().resumeId(resumeId).rawText("Java Spring Boot").build();
    ResumeExtractionResult extracted =
        new ResumeExtractionResult(
            List.of("Java", "Spring Boot"),
            4.0,
            EducationLevel.BACHELOR,
            List.of("Backend Developer"),
            List.of("Software"));
    ResumeExtractionResult normalized =
        new ResumeExtractionResult(
            List.of("Java", "Spring Boot"),
            4.0,
            EducationLevel.BACHELOR,
            List.of("Backend Developer"),
            List.of("Software"));
    StructuredResume saved =
        StructuredResume.builder()
            .resumeId(resumeId)
            .skills(normalized.skills())
            .totalYearsExperience(normalized.totalYearsExperience())
            .highestEducationLevel(normalized.highestEducationLevel())
            .jobTitles(normalized.jobTitles())
            .domains(normalized.domains())
            .extractionModel("gemini-3.5-flash")
            .build();

    when(structuredResumeRepository.findById(resumeId)).thenReturn(Optional.empty());
    when(resumeParsingService.parseAndPersist(resumeId)).thenReturn(parsedResume);
    when(resumeStructuredExtractor.extract(parsedResume.getRawText())).thenReturn(extracted);
    when(resumeExtractionNormalizer.normalize(extracted)).thenReturn(normalized);
    when(structuredResumeRepository.save(any(StructuredResume.class))).thenReturn(saved);

    StructuredResume result = resumeAnalysisService.analyzeAndPersist(resumeId);

    assertSame(saved, result);
    verify(resumeParsingService).parseAndPersist(resumeId);
    verify(resumeStructuredExtractor).extract(parsedResume.getRawText());
    verify(resumeExtractionNormalizer).normalize(extracted);
    verify(structuredResumeRepository).save(any(StructuredResume.class));
  }

  @Test
  void returnsExistingStructuredResumeWithoutReprocessing() {
    UUID resumeId = UUID.randomUUID();
    StructuredResume existing =
        StructuredResume.builder()
            .resumeId(resumeId)
            .skills(List.of("Java"))
            .totalYearsExperience(3.0)
            .highestEducationLevel(EducationLevel.BACHELOR)
            .jobTitles(List.of("Developer"))
            .domains(List.of("Software"))
            .analyzedAt(Instant.now())
            .extractionModel("gemini-3.5-flash")
            .build();

    when(structuredResumeRepository.findById(resumeId)).thenReturn(Optional.of(existing));

    StructuredResume result = resumeAnalysisService.analyzeAndPersist(resumeId);

    assertSame(existing, result);
    verifyNoInteractions(resumeParsingService);
    verifyNoInteractions(resumeStructuredExtractor);
    verifyNoInteractions(resumeExtractionNormalizer);
    verify(structuredResumeRepository, never()).save(any(StructuredResume.class));
  }

  @Test
  void rejectsBlankRawText() {
    UUID resumeId = UUID.randomUUID();

    when(structuredResumeRepository.findById(resumeId)).thenReturn(Optional.empty());
    when(resumeParsingService.parseAndPersist(resumeId))
        .thenReturn(ParsedResume.builder().resumeId(resumeId).rawText("   ").build());

    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class, () -> resumeAnalysisService.analyzeAndPersist(resumeId));

    assertEquals("Resume raw text is blank", exception.getMessage());
    verifyNoInteractions(resumeStructuredExtractor);
    verifyNoInteractions(resumeExtractionNormalizer);
    verify(structuredResumeRepository, never()).save(any(StructuredResume.class));
  }

  @Test
  void doesNotPersistWhenGeminiExtractionFails() {
    UUID resumeId = UUID.randomUUID();
    ParsedResume parsedResume = ParsedResume.builder().resumeId(resumeId).rawText("Java").build();

    when(structuredResumeRepository.findById(resumeId)).thenReturn(Optional.empty());
    when(resumeParsingService.parseAndPersist(resumeId)).thenReturn(parsedResume);
    when(resumeStructuredExtractor.extract(parsedResume.getRawText()))
        .thenThrow(new IllegalStateException("gemini failure"));

    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class, () -> resumeAnalysisService.analyzeAndPersist(resumeId));

    assertEquals("gemini failure", exception.getMessage());
    verify(resumeExtractionNormalizer, never()).normalize(any(ResumeExtractionResult.class));
    verify(structuredResumeRepository, never()).save(any(StructuredResume.class));
  }

  @Test
  void doesNotPersistWhenNormalizedResultIsInvalid() {
    UUID resumeId = UUID.randomUUID();
    ParsedResume parsedResume = ParsedResume.builder().resumeId(resumeId).rawText("Java").build();
    ResumeExtractionResult extracted =
        new ResumeExtractionResult(
            List.of("Java"), 4.0, EducationLevel.BACHELOR, List.of("Developer"), List.of("Tech"));

    when(structuredResumeRepository.findById(resumeId)).thenReturn(Optional.empty());
    when(resumeParsingService.parseAndPersist(resumeId)).thenReturn(parsedResume);
    when(resumeStructuredExtractor.extract(parsedResume.getRawText())).thenReturn(extracted);
    when(resumeExtractionNormalizer.normalize(extracted))
        .thenThrow(new IllegalArgumentException("invalid extraction"));

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> resumeAnalysisService.analyzeAndPersist(resumeId));

    assertEquals("invalid extraction", exception.getMessage());
    verify(structuredResumeRepository, never()).save(any(StructuredResume.class));
  }
}
