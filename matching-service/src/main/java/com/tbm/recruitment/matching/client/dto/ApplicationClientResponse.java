package com.tbm.recruitment.matching.client.dto;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationClientResponse {

  private UUID id;
  private UUID candidateId;
  private UUID jobId;
  private UUID resumeId;
  private String status;
  private Instant submittedAt;
}
