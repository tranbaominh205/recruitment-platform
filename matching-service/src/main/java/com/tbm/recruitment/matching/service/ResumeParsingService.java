package com.tbm.recruitment.matching.service;

import com.tbm.recruitment.matching.client.ResumeServiceClient;
import com.tbm.recruitment.matching.document.ParsedResume;
import com.tbm.recruitment.matching.extractor.PdfTextExtractor;
import com.tbm.recruitment.matching.repository.ParsedResumeRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResumeParsingService {

  private final ParsedResumeRepository parsedResumeRepository;
  private final ResumeServiceClient resumeServiceClient;
  private final PdfTextExtractor pdfTextExtractor;

  public ParsedResume parseAndPersist(UUID resumeId) {
    if (resumeId == null) {
      throw new IllegalArgumentException("resumeId is required");
    }

    return parsedResumeRepository
        .findById(resumeId)
        .orElseGet(
            () -> {
              byte[] pdfBytes = resumeServiceClient.fetchPdfContent(resumeId);
              String rawText = pdfTextExtractor.extract(pdfBytes);

              ParsedResume parsedResume =
                  ParsedResume.builder()
                      .resumeId(resumeId)
                      .rawText(rawText)
                      .extractedAt(Instant.now())
                      .build();

              return parsedResumeRepository.save(parsedResume);
            });
  }
}
