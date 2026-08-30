package com.tbm.recruitment.identity.service;

import com.tbm.recruitment.identity.dto.request.IntrospectRequest;
import com.tbm.recruitment.identity.dto.request.LoginRequest;
import com.tbm.recruitment.identity.dto.request.RegisterRequest;
import com.tbm.recruitment.identity.dto.response.AccountResponse;
import com.tbm.recruitment.identity.dto.response.IntrospectResponse;
import com.tbm.recruitment.identity.dto.response.LoginResponse;
import com.tbm.recruitment.identity.entity.Account;
import com.tbm.recruitment.identity.entity.Role;
import com.tbm.recruitment.identity.exception.AppException;
import com.tbm.recruitment.identity.exception.ErrorCode;
import com.tbm.recruitment.identity.repository.AccountRepository;
import com.tbm.recruitment.identity.security.JwtService;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationService {

  AccountRepository accountRepository;
  PasswordEncoder passwordEncoder;
  JwtService jwtService;

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

  @Transactional(readOnly = true)
  public LoginResponse login(LoginRequest request) {

    String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);

    Account account =
        accountRepository
            .findByEmailIgnoreCase(normalizedEmail)
            .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));

    if (!account.isEnabled()) {
      throw new AppException(ErrorCode.ACCOUNT_DISABLED);
    }

    if (!passwordEncoder.matches(request.password(), account.getPasswordHash())) {
      throw new AppException(ErrorCode.INVALID_CREDENTIALS);
    }

    String accessToken = jwtService.generateAccessToken(account);

    return new LoginResponse(accessToken, "Bearer", 3600);
  }

  public IntrospectResponse introspect(IntrospectRequest request) {
    return jwtService.introspect(request.token());
  }
}
