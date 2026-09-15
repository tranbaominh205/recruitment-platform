package com.tbm.recruitment.identity.service;

import com.tbm.recruitment.identity.dto.request.ChangePasswordRequest;
import com.tbm.recruitment.identity.dto.response.AccountResponse;
import com.tbm.recruitment.identity.dto.response.AdminAccountStatisticsResponse;
import com.tbm.recruitment.identity.dto.response.MeResponse;
import com.tbm.recruitment.identity.dto.response.PageResponse;
import com.tbm.recruitment.identity.entity.Account;
import com.tbm.recruitment.identity.entity.Role;
import com.tbm.recruitment.identity.exception.AppException;
import com.tbm.recruitment.identity.exception.ErrorCode;
import com.tbm.recruitment.identity.mapper.AccountMapper;
import com.tbm.recruitment.identity.repository.AccountRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AccountService {

  AccountRepository accountRepository;
  AccountMapper accountMapper;
  PasswordEncoder passwordEncoder;

  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('ADMIN')")
  public PageResponse<AccountResponse> getAccounts(
      String keyword, String role, Boolean enabled, int page, int size) {
    validatePagination(page, size);
    Role roleFilter = parseRole(role);

    Specification<Account> specification =
        (root, query, criteriaBuilder) -> {
          List<Predicate> predicates = new java.util.ArrayList<>();

          if (StringUtils.hasText(keyword)) {
            predicates.add(
                criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("email")),
                    "%" + keyword.trim().toLowerCase() + "%"));
          }

          if (roleFilter != null) {
            predicates.add(criteriaBuilder.equal(root.get("role"), roleFilter));
          }

          if (enabled != null) {
            predicates.add(criteriaBuilder.equal(root.get("enabled"), enabled));
          }

          return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };

    PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    Page<Account> accountPage = accountRepository.findAll(specification, pageRequest);
    List<AccountResponse> content =
        accountPage.getContent().stream().map(accountMapper::toAccountResponse).toList();

    return new PageResponse<>(
        content,
        accountPage.getNumber(),
        accountPage.getSize(),
        accountPage.getTotalElements(),
        accountPage.getTotalPages());
  }

  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('ADMIN')")
  public AccountResponse getAccountById(UUID accountId) {
    Account account =
        accountRepository
            .findById(accountId)
            .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));
    return accountMapper.toAccountResponse(account);
  }

  @Transactional
  @PreAuthorize("hasRole('ADMIN')")
  public AccountResponse updateAccountEnabled(UUID accountId, boolean enabled, Jwt jwt) {
    if (jwt == null) {
      throw new AppException(ErrorCode.UNAUTHENTICATED);
    }
    UUID currentAdminId = parseUuidOrUnauthenticated(jwt.getSubject());

    if (!enabled && currentAdminId.equals(accountId)) {
      throw new AppException(ErrorCode.UNAUTHORIZED);
    }

    Account account =
        accountRepository
            .findById(accountId)
            .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));

    if (account.isEnabled() != enabled) {
      account.setEnabled(enabled);
      account.setTokenVersion(account.getTokenVersion() + 1);
      account = accountRepository.save(account);
    }

    return accountMapper.toAccountResponse(account);
  }

  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('ADMIN')")
  public AdminAccountStatisticsResponse getAdminStatistics() {
    long total = accountRepository.count();
    long candidate = accountRepository.countByRole(Role.CANDIDATE);
    long recruiter = accountRepository.countByRole(Role.RECRUITER);
    long admin = accountRepository.countByRole(Role.ADMIN);
    long enabled = accountRepository.countByEnabled(true);

    return new AdminAccountStatisticsResponse(
        total, candidate, recruiter, admin, enabled, total - enabled);
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
    UUID accountId = parseUuidOrUnauthenticated(jwt.getSubject());

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

  private UUID parseUuidOrUnauthenticated(String rawAccountId) {
    try {
      return UUID.fromString(rawAccountId);
    } catch (IllegalArgumentException exception) {
      throw new AppException(ErrorCode.UNAUTHENTICATED);
    }
  }

  private void validatePagination(int page, int size) {
    if (page < 0 || size < 1 || size > 100) {
      throw new AppException(ErrorCode.INVALID_REQUEST);
    }
  }

  private Role parseRole(String role) {
    if (!StringUtils.hasText(role)) {
      return null;
    }

    try {
      return Role.valueOf(role.trim().toUpperCase());
    } catch (IllegalArgumentException exception) {
      throw new AppException(ErrorCode.INVALID_REQUEST);
    }
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
