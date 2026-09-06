package com.tbm.recruitment.matching.client.dto;

import com.tbm.recruitment.matching.model.EducationLevel;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JobClientResponse {

  private UUID id;
  private String title;
  private List<String> requiredSkills;
  private Integer minimumYearsExperience;
  private EducationLevel requiredEducationLevel;
  private String domain;
}
