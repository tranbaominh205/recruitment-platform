package com.tbm.recruitment.identity.service;

import com.tbm.recruitment.identity.dto.request.RegisterRequest;
import com.tbm.recruitment.identity.dto.response.AccountResponse;
import com.tbm.recruitment.identity.entity.Account;
import com.tbm.recruitment.identity.entity.Role;
import com.tbm.recruitment.identity.exception.AppException;
import com.tbm.recruitment.identity.exception.ErrorCode;
import com.tbm.recruitment.identity.repository.AccountRepository;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

  private final AccountRepository accountRepository;
  private final PasswordEncoder passwordEncoder;

  @Transactional
  public AccountResponse register(RegisterRequest request) {

    String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);

    if (accountRepository.existsByEmailIgnoreCase(normalizedEmail)) {
      throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
    }

    if (request.role() == Role.ADMIN) {
      throw new AppException(ErrorCode.ADMIN_REGISTRATION_NOT_ALLOWED);
    }

    Account account =
        Account.builder()
            .email(normalizedEmail)
            .passwordHash(passwordEncoder.encode(request.password()))
            .role(request.role())
            .enabled(true)
            .build();

    account = accountRepository.save(account);

    return new AccountResponse(
        account.getId(),
        account.getEmail(),
        account.getRole(),
        account.isEnabled(),
        account.getCreatedAt());
  }
}
