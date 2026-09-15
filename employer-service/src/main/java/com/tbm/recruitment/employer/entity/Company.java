package com.tbm.recruitment.employer.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(
    name = "companies",
    uniqueConstraints = {
      @UniqueConstraint(name = "uk_companies_owner_account_id", columnNames = "owner_account_id")
    })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Company {

  @Id private UUID id;

  @Column(name = "owner_account_id", nullable = false, updatable = false)
  private UUID ownerAccountId;

  @Column(nullable = false, length = 200)
  private String name;

  @Column(length = 2000)
  private String description;

  @Column(length = 255)
  private String website;

  @Column(length = 150)
  private String industry;

  @Column(length = 200)
  private String location;

  @Enumerated(EnumType.STRING)
  @Column(name = "moderation_status", length = 30)
  private CompanyModerationStatus moderationStatus;

  @Column(name = "moderation_reason", length = 1000)
  private String moderationReason;

  @Column(name = "moderated_by_account_id")
  private UUID moderatedByAccountId;

  @Column(name = "moderated_at")
  private Instant moderatedAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "verification_status", length = 30)
  private CompanyVerificationStatus verificationStatus;

  @Column(name = "verification_reason", length = 1000)
  private String verificationReason;

  @Column(name = "verified_by_account_id")
  private UUID verifiedByAccountId;

  @Column(name = "verified_at")
  private Instant verifiedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @PrePersist
  void prePersist() {
    if (id == null) {
      id = UUID.randomUUID();
    }

    if (moderationStatus == null) {
      moderationStatus = CompanyModerationStatus.ACTIVE;
    }

    if (verificationStatus == null) {
      verificationStatus = CompanyVerificationStatus.UNVERIFIED;
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
