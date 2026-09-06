package com.tbm.recruitment.matching.document;

import com.tbm.recruitment.matching.model.EducationLevel;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "structured_resumes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StructuredResume {

  @Id private UUID resumeId;

  private List<String> skills;

  private double totalYearsExperience;

  private EducationLevel highestEducationLevel;

  private List<String> jobTitles;

  private List<String> domains;

  private Instant analyzedAt;

  private String extractionModel;
}
