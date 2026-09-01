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
