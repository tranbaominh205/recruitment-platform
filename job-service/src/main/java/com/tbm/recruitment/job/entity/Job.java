package com.tbm.recruitment.job.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "jobs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Job {

  @Id private UUID id;

  @Column(name = "company_id", nullable = false, updatable = false)
  private UUID companyId;

  @Column(name = "created_by_account_id", nullable = false, updatable = false)
  private UUID createdByAccountId;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(nullable = false, length = 5000)
  private String description;

  @Column(length = 5000)
  private String requirements;

  @Column(length = 200)
  private String location;

  @Column(name = "employment_type", length = 50)
  private String employmentType;

  @Column(name = "workplace_type", length = 50)
  private String workplaceType;

  @Column(name = "salary_min", precision = 15, scale = 2)
  private BigDecimal salaryMin;

  @Column(name = "salary_max", precision = 15, scale = 2)
  private BigDecimal salaryMax;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private JobStatus status;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @PrePersist
  void prePersist() {
    if (id == null) {
      id = UUID.randomUUID();
    }

    if (status == null) {
      status = JobStatus.DRAFT;
    }

    Instant now = Instant.now();

    if (createdAt == null) {
      createdAt = now;
    }

    if (updatedAt == null) {
      updatedAt = now;
    }
  }

  @PreUpdate
  void preUpdate() {
    updatedAt = Instant.now();
  }
}
