package com.tbm.recruitment.identity.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tbm.recruitment.identity.entity.Account;
import com.tbm.recruitment.identity.entity.Role;
import com.tbm.recruitment.identity.repository.AccountRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class AdminBootstrapServiceTest {

  private AccountRepository accountRepository;
  private PasswordEncoder passwordEncoder;
  private AdminBootstrapService adminBootstrapService;

  @BeforeEach
  void setUp() {
    accountRepository = mock(AccountRepository.class);
    passwordEncoder = new BCryptPasswordEncoder();
    adminBootstrapService = new AdminBootstrapService(accountRepository, passwordEncoder);
  }

  @Test
  void bootstrapDisabledDoesNothing() throws Exception {
    ReflectionTestUtils.setField(adminBootstrapService, "adminBootstrapEnabled", false);

    adminBootstrapService.run(new DefaultApplicationArguments(new String[0]));

    verify(accountRepository, never()).findByEmailIgnoreCase(any());
    verify(accountRepository, never()).save(any(Account.class));
  }

  @Test
  void bootstrapEnabledCreatesAdminWhenAbsent() throws Exception {
    ReflectionTestUtils.setField(adminBootstrapService, "adminBootstrapEnabled", true);
    ReflectionTestUtils.setField(
        adminBootstrapService, "adminBootstrapEmail", "  ADMIN@EXAMPLE.COM ");
    ReflectionTestUtils.setField(
        adminBootstrapService, "adminBootstrapPassword", "admin-secret-123");
    when(accountRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.empty());
    when(accountRepository.save(any(Account.class)))
        .thenAnswer(
            invocation -> {
              Account saved = invocation.getArgument(0);
              if (saved.getId() == null) {
                saved.setId(UUID.randomUUID());
              }
              if (saved.getCreatedAt() == null) {
                saved.setCreatedAt(Instant.now());
              }
              return saved;
            });

    adminBootstrapService.run(new DefaultApplicationArguments(new String[0]));

    verify(accountRepository)
        .save(
            argThat(
                account ->
                    account.getEmail().equals("admin@example.com")
                        && account.getRole() == Role.ADMIN
                        && account.isEnabled()
                        && account.getTokenVersion().equals(0L)
                        && passwordEncoder.matches("admin-secret-123", account.getPasswordHash())));
  }

  @Test
  void bootstrapEnabledIsIdempotentForExistingAdminAndDoesNotResetPasswordOrEnabled()
      throws Exception {
    ReflectionTestUtils.setField(adminBootstrapService, "adminBootstrapEnabled", true);
    ReflectionTestUtils.setField(adminBootstrapService, "adminBootstrapEmail", "admin@example.com");
    ReflectionTestUtils.setField(
        adminBootstrapService, "adminBootstrapPassword", "admin-secret-123");

    Account existingAdmin =
        Account.builder()
            .id(UUID.randomUUID())
            .email("admin@example.com")
            .passwordHash("existing-hash")
            .role(Role.ADMIN)
            .enabled(false)
            .tokenVersion(12L)
            .createdAt(Instant.now())
            .build();

    when(accountRepository.findByEmailIgnoreCase("admin@example.com"))
        .thenReturn(Optional.of(existingAdmin));

    adminBootstrapService.run(new DefaultApplicationArguments(new String[0]));

    assertEquals("existing-hash", existingAdmin.getPasswordHash());
    assertEquals(false, existingAdmin.isEnabled());
    assertEquals(12L, existingAdmin.getTokenVersion());
    verify(accountRepository, never()).save(any(Account.class));
  }

  @Test
  void bootstrapEnabledFailsWhenNonAdminExistsWithSameEmail() {
    ReflectionTestUtils.setField(adminBootstrapService, "adminBootstrapEnabled", true);
    ReflectionTestUtils.setField(adminBootstrapService, "adminBootstrapEmail", "admin@example.com");
    ReflectionTestUtils.setField(
        adminBootstrapService, "adminBootstrapPassword", "admin-secret-123");

    Account existingCandidate =
        Account.builder()
            .id(UUID.randomUUID())
            .email("admin@example.com")
            .passwordHash("hash")
            .role(Role.CANDIDATE)
            .enabled(true)
            .tokenVersion(0L)
            .createdAt(Instant.now())
            .build();
    when(accountRepository.findByEmailIgnoreCase("admin@example.com"))
        .thenReturn(Optional.of(existingCandidate));

    assertThrows(
        IllegalStateException.class,
        () -> adminBootstrapService.run(new DefaultApplicationArguments(new String[0])));
    verify(accountRepository, never()).save(any(Account.class));
  }
}
