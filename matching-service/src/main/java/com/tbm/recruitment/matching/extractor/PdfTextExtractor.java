package com.tbm.recruitment.matching.extractor;

import java.io.IOException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

@Component
public class PdfTextExtractor {

  public String extract(byte[] pdfBytes) {
    if (pdfBytes == null || pdfBytes.length == 0) {
      throw new IllegalArgumentException("PDF bytes are required");
    }

    try (PDDocument document = Loader.loadPDF(pdfBytes)) {
      PDFTextStripper stripper = new PDFTextStripper();
      String text = stripper.getText(document);
      if (text == null || text.isBlank()) {
        throw new IllegalStateException("Extracted PDF text is blank");
      }
      return text.trim();
    } catch (IOException exception) {
      throw new IllegalStateException("Failed to extract text from PDF", exception);
    }
  }
}
