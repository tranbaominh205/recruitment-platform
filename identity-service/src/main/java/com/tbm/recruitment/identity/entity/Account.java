package com.tbm.recruitment.identity.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.*;

@Entity
@Table(
    name = "accounts",
    uniqueConstraints = {@UniqueConstraint(name = "uk_accounts_email", columnNames = "email")})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account {

  @Id private UUID id;

  @Column(nullable = false, length = 150)
  private String email;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private Role role;

  @ElementCollection(fetch = FetchType.LAZY, targetClass = AdminPermission.class)
  @Enumerated(EnumType.STRING)
  @CollectionTable(
      name = "account_admin_permissions",
      joinColumns = @JoinColumn(name = "account_id"))
  @Column(name = "permission", nullable = false, length = 50)
  @Builder.Default
  private Set<AdminPermission> adminPermissions = new HashSet<>();

  @Column(nullable = false)
  private boolean enabled;

  @Column(name = "token_version", nullable = false, columnDefinition = "BIGINT DEFAULT 0")
  private Long tokenVersion;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @PrePersist
  void prePersist() {
    if (id == null) {
      id = UUID.randomUUID();
    }

    if (createdAt == null) {
      createdAt = Instant.now();
    }

    if (tokenVersion == null) {
      tokenVersion = 0L;
    }

    if (adminPermissions == null) {
      adminPermissions = new HashSet<>();
    }
  }
}
