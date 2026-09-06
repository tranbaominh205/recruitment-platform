package com.tbm.recruitment.matching.extractor;

import com.tbm.recruitment.matching.model.ResumeExtractionResult;

public interface ResumeStructuredExtractor {
  ResumeExtractionResult extract(String rawText);
}
