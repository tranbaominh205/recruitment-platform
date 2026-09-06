package com.tbm.recruitment.recruitment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "interviews",
    uniqueConstraints =
        @UniqueConstraint(name = "uk_interview_application", columnNames = "application_id"))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Interview {

  @Id private UUID id;

  @Column(name = "application_id", nullable = false, updatable = false)
  private UUID applicationId;

  @Column(name = "scheduled_at", nullable = false)
  private Instant scheduledAt;

  @Column(nullable = false, length = 500)
  private String location;

  @Column(length = 1000)
  private String note;

  @Column(name = "scheduled_by_account_id", nullable = false, updatable = false)
  private UUID scheduledByAccountId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @PrePersist
  void prePersist() {

    if (id == null) {
      id = UUID.randomUUID();
    }

    if (createdAt == null) {
      createdAt = Instant.now();
    }
  }
}
