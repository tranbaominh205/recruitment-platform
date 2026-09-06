package com.tbm.recruitment.matching.extractor;

import com.tbm.recruitment.matching.document.StructuredResume;
import com.tbm.recruitment.matching.model.JobMatchingCriteria;
import com.tbm.recruitment.matching.model.MatchExplanation;
import com.tbm.recruitment.matching.model.MatchScoreResult;

public interface MatchExplanationGenerator {

  MatchExplanation generate(
      JobMatchingCriteria jobMatchingCriteria,
      StructuredResume structuredResume,
      MatchScoreResult matchScoreResult);
}
