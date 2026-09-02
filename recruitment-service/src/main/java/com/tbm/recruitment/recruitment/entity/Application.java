package com.tbm.recruitment.recruitment.entity;

import com.tbm.recruitment.recruitment.enums.ApplicationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "applications",
    indexes = {
      @Index(name = "idx_application_candidate", columnList = "candidate_id"),
      @Index(name = "idx_application_job", columnList = "job_id")
    })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Application {

  @Id private UUID id;

  @Column(name = "candidate_id", nullable = false, updatable = false)
  private UUID candidateId;

  @Column(name = "job_id", nullable = false, updatable = false)
  private UUID jobId;

  @Column(name = "resume_id", nullable = false, updatable = false)
  private UUID resumeId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private ApplicationStatus status;

  @Column(name = "submitted_at", nullable = false, updatable = false)
  private Instant submittedAt;

  @PrePersist
  void prePersist() {

    if (id == null) {
      id = UUID.randomUUID();
    }

    if (status == null) {
      status = ApplicationStatus.SUBMITTED;
    }

    if (submittedAt == null) {
      submittedAt = Instant.now();
    }
  }
}
