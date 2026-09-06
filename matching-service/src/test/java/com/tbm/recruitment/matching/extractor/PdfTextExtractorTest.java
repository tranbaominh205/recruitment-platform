package com.tbm.recruitment.matching.extractor;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

class PdfTextExtractorTest {

  private final PdfTextExtractor extractor = new PdfTextExtractor();

  @Test
  void extractsTextFromValidPdf() throws IOException {
    byte[] pdfBytes = createPdf("Known candidate text");

    String extracted = extractor.extract(pdfBytes);

    assertTrue(extracted.contains("Known candidate text"));
  }

  @Test
  void failsForCorruptPdfBytes() {
    IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> extractor.extract(new byte[] {1, 2, 3, 4}));

    assertTrue(exception.getMessage().contains("Failed to extract text from PDF"));
  }

  @Test
  void rejectsBlankPdfText() throws IOException {
    byte[] pdfBytes = createPdf("");

    IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> extractor.extract(pdfBytes));

    assertTrue(exception.getMessage().contains("blank"));
  }

  private byte[] createPdf(String text) throws IOException {
    try (PDDocument document = new PDDocument();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      PDPage page = new PDPage();
      document.addPage(page);

      if (!text.isBlank()) {
        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
          contentStream.beginText();
          contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
          contentStream.newLineAtOffset(100, 700);
          contentStream.showText(text);
          contentStream.endText();
        }
      }

      document.save(outputStream);
      return outputStream.toByteArray();
    }
  }
}
