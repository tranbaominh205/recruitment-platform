package com.tbm.recruitment.identity.service;

import com.tbm.recruitment.identity.dto.request.ChangePasswordRequest;
import com.tbm.recruitment.identity.dto.response.AccountResponse;
import com.tbm.recruitment.identity.dto.response.MeResponse;
import com.tbm.recruitment.identity.entity.Account;
import com.tbm.recruitment.identity.exception.AppException;
import com.tbm.recruitment.identity.exception.ErrorCode;
import com.tbm.recruitment.identity.mapper.AccountMapper;
import com.tbm.recruitment.identity.repository.AccountRepository;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AccountService {

  AccountRepository accountRepository;
  AccountMapper accountMapper;
  PasswordEncoder passwordEncoder;

  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('ADMIN')")
  public List<AccountResponse> getAccounts() {
    return accountRepository.findAll().stream().map(accountMapper::toAccountResponse).toList();
  }

  @Transactional(readOnly = true)
  public MeResponse getCurrentAccount(Jwt jwt) {
    Account account = loadAuthoritativeEnabledAccount(jwt);
    return new MeResponse(account.getId().toString(), account.getEmail(), account.getRole().name());
  }

  @Transactional
  public void changePassword(Jwt jwt, ChangePasswordRequest request) {
    Account account = loadAuthoritativeEnabledAccount(jwt);

    if (!passwordEncoder.matches(request.currentPassword(), account.getPasswordHash())) {
      throw new AppException(ErrorCode.INVALID_CURRENT_PASSWORD);
    }

    account.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    account.setTokenVersion(account.getTokenVersion() + 1);
    accountRepository.save(account);
  }

  private Account loadAuthoritativeEnabledAccount(Jwt jwt) {
    String subject = jwt.getSubject();
    UUID accountId;

    try {
      accountId = UUID.fromString(subject);
    } catch (IllegalArgumentException exception) {
      throw new AppException(ErrorCode.UNAUTHENTICATED);
    }

    Account account =
        accountRepository
            .findById(accountId)
            .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

    if (!account.isEnabled()) {
      throw new AppException(ErrorCode.UNAUTHENTICATED);
    }

    Long accountTokenVersion = account.getTokenVersion();
    Long tokenVersion = parseTokenVersionClaim(jwt.getClaims().get("tokenVersion"));
    if (accountTokenVersion == null
        || tokenVersion == null
        || !accountTokenVersion.equals(tokenVersion)) {
      throw new AppException(ErrorCode.UNAUTHENTICATED);
    }

    return account;
  }

  private Long parseTokenVersionClaim(Object tokenVersionClaim) {
    if (tokenVersionClaim == null) {
      return null;
    }

    if (tokenVersionClaim instanceof Number number) {
      if (number.doubleValue() != (double) number.longValue()) {
        return null;
      }
      return number.longValue();
    }

    if (tokenVersionClaim instanceof String value) {
      try {
        return Long.parseLong(value);
      } catch (NumberFormatException exception) {
        return null;
      }
    }

    return null;
  }
}
