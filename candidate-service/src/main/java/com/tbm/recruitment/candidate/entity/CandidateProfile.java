package com.tbm.recruitment.candidate.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(
    name = "candidate_profiles",
    uniqueConstraints = {
      @UniqueConstraint(name = "uk_candidate_profiles_account_id", columnNames = "account_id")
    })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateProfile {

  @Id private UUID id;

  @Column(name = "account_id", nullable = false, updatable = false)
  private UUID accountId;

  @Column(name = "full_name", nullable = false, length = 150)
  private String fullName;

  @Column(length = 30)
  private String phone;

  @Column(length = 200)
  private String school;

  @Column(length = 150)
  private String major;

  @Column(name = "graduation_year")
  private Integer graduationYear;

  @Column(length = 150)
  private String location;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @PrePersist
  void prePersist() {
    if (id == null) {
      id = UUID.randomUUID();
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
