package com.tbm.recruitment.matching.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tbm.recruitment.matching.client.ResumeServiceClient;
import com.tbm.recruitment.matching.document.ParsedResume;
import com.tbm.recruitment.matching.extractor.PdfTextExtractor;
import com.tbm.recruitment.matching.repository.ParsedResumeRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResumeParsingServiceTest {

  @Mock private ParsedResumeRepository parsedResumeRepository;
  @Mock private ResumeServiceClient resumeServiceClient;
  @Mock private PdfTextExtractor pdfTextExtractor;

  @InjectMocks private ResumeParsingService resumeParsingService;

  @Test
  void parsesAndPersistsNewResume() {
    UUID resumeId = UUID.randomUUID();
    byte[] pdfBytes = new byte[] {1, 2, 3};

    when(parsedResumeRepository.findById(resumeId)).thenReturn(Optional.empty());
    when(resumeServiceClient.fetchPdfContent(resumeId)).thenReturn(pdfBytes);
    when(pdfTextExtractor.extract(pdfBytes)).thenReturn("candidate experience");
    when(parsedResumeRepository.save(any(ParsedResume.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    ParsedResume result = resumeParsingService.parseAndPersist(resumeId);

    assertEquals(resumeId, result.getResumeId());
    assertEquals("candidate experience", result.getRawText());
    verify(resumeServiceClient).fetchPdfContent(resumeId);
    verify(pdfTextExtractor).extract(pdfBytes);
    verify(parsedResumeRepository).save(any(ParsedResume.class));
  }

  @Test
  void returnsExistingParsedResumeWithoutDuplicatePersistence() {
    UUID resumeId = UUID.randomUUID();
    ParsedResume existing =
        ParsedResume.builder()
            .resumeId(resumeId)
            .rawText("cached text")
            .extractedAt(Instant.now())
            .build();

    when(parsedResumeRepository.findById(resumeId)).thenReturn(Optional.of(existing));

    ParsedResume result = resumeParsingService.parseAndPersist(resumeId);

    assertSame(existing, result);
    verifyNoInteractions(resumeServiceClient);
    verifyNoInteractions(pdfTextExtractor);
    verify(parsedResumeRepository, never()).save(any(ParsedResume.class));
  }

  @Test
  void rejectsBlankExtraction() {
    UUID resumeId = UUID.randomUUID();
    byte[] pdfBytes = new byte[] {1, 2, 3};

    when(parsedResumeRepository.findById(resumeId)).thenReturn(Optional.empty());
    when(resumeServiceClient.fetchPdfContent(resumeId)).thenReturn(pdfBytes);
    when(pdfTextExtractor.extract(pdfBytes)).thenThrow(new IllegalStateException("blank"));

    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class, () -> resumeParsingService.parseAndPersist(resumeId));

    assertEquals("blank", exception.getMessage());
    verify(parsedResumeRepository, never()).save(any(ParsedResume.class));
  }

  @Test
  void doesNotPersistWhenResumeClientFails() {
    UUID resumeId = UUID.randomUUID();

    when(parsedResumeRepository.findById(resumeId)).thenReturn(Optional.empty());
    when(resumeServiceClient.fetchPdfContent(resumeId))
        .thenThrow(new IllegalStateException("upstream failure"));

    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class, () -> resumeParsingService.parseAndPersist(resumeId));

    assertEquals("upstream failure", exception.getMessage());
    verifyNoInteractions(pdfTextExtractor);
    verify(parsedResumeRepository, never()).save(any(ParsedResume.class));
  }
}
