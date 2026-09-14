package com.tbm.recruitment.identity.service;

import com.tbm.recruitment.identity.entity.Account;
import com.tbm.recruitment.identity.entity.Role;
import com.tbm.recruitment.identity.repository.AccountRepository;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminBootstrapService implements ApplicationRunner {

  private final AccountRepository accountRepository;
  private final PasswordEncoder passwordEncoder;

  @Value("${identity.bootstrap.admin.enabled:false}")
  private boolean adminBootstrapEnabled;

  @Value("${identity.bootstrap.admin.email:}")
  private String adminBootstrapEmail;

  @Value("${identity.bootstrap.admin.password:}")
  private String adminBootstrapPassword;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (!adminBootstrapEnabled) {
      return;
    }

    String normalizedEmail = normalizeBootstrapEmail(adminBootstrapEmail);
    validateBootstrapPassword(adminBootstrapPassword);

    Optional<Account> existingAccount = accountRepository.findByEmailIgnoreCase(normalizedEmail);
    if (existingAccount.isPresent()) {
      if (existingAccount.get().getRole() == Role.ADMIN) {
        return;
      }
      throw new IllegalStateException(
          "ADMIN bootstrap failed: non-ADMIN account already exists with bootstrap email.");
    }

    Account adminAccount =
        Account.builder()
            .email(normalizedEmail)
            .passwordHash(passwordEncoder.encode(adminBootstrapPassword))
            .role(Role.ADMIN)
            .enabled(true)
            .tokenVersion(0L)
            .build();

    accountRepository.save(adminAccount);
  }

  private String normalizeBootstrapEmail(String rawEmail) {
    if (rawEmail == null) {
      throw new IllegalStateException("ADMIN bootstrap failed: ADMIN_BOOTSTRAP_EMAIL is required.");
    }

    String normalizedEmail = rawEmail.trim().toLowerCase(Locale.ROOT);
    if (normalizedEmail.isBlank()) {
      throw new IllegalStateException("ADMIN bootstrap failed: ADMIN_BOOTSTRAP_EMAIL is required.");
    }

    return normalizedEmail;
  }

  private void validateBootstrapPassword(String rawPassword) {
    if (rawPassword == null || rawPassword.isBlank()) {
      throw new IllegalStateException(
          "ADMIN bootstrap failed: ADMIN_BOOTSTRAP_PASSWORD is required.");
    }

    if (rawPassword.length() < 8 || rawPassword.length() > 72) {
      throw new IllegalStateException(
          "ADMIN bootstrap failed: ADMIN_BOOTSTRAP_PASSWORD length must be between 8 and 72.");
    }
  }
}
