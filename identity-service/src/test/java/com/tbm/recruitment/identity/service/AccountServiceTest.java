package com.tbm.recruitment.identity.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tbm.recruitment.identity.dto.request.ChangePasswordRequest;
import com.tbm.recruitment.identity.dto.request.IntrospectRequest;
import com.tbm.recruitment.identity.dto.request.RefreshRequest;
import com.tbm.recruitment.identity.dto.response.AccountResponse;
import com.tbm.recruitment.identity.dto.response.AdminAccountStatisticsResponse;
import com.tbm.recruitment.identity.dto.response.IntrospectResponse;
import com.tbm.recruitment.identity.dto.response.MeResponse;
import com.tbm.recruitment.identity.dto.response.PageResponse;
import com.tbm.recruitment.identity.entity.Account;
import com.tbm.recruitment.identity.entity.Role;
import com.tbm.recruitment.identity.exception.AppException;
import com.tbm.recruitment.identity.exception.ErrorCode;
import com.tbm.recruitment.identity.mapper.AccountMapper;
import com.tbm.recruitment.identity.repository.AccountRepository;
import com.tbm.recruitment.identity.repository.InvalidatedTokenRepository;
import com.tbm.recruitment.identity.security.JwtService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class AccountServiceTest {

  private AccountService accountService;
  private AuthenticationService authenticationService;
  private AccountRepository accountRepository;
  private InvalidatedTokenRepository invalidatedTokenRepository;
  private PasswordEncoder passwordEncoder;
  private JwtService jwtService;
  private JwtEncoder jwtEncoder;
  private AccountMapper accountMapper;

  @BeforeEach
  void setUp() {
    SecretKey secretKey =
        new SecretKeySpec("0123456789abcdef0123456789abcdef".getBytes(), "HmacSHA256");
    jwtEncoder = NimbusJwtEncoder.withSecretKey(secretKey).build();
    JwtDecoder jwtDecoder =
        NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build();
    ((NimbusJwtDecoder) jwtDecoder)
        .setJwtValidator(JwtValidators.createDefaultWithIssuer("identity-service"));
    JwtDecoder refreshJwtDecoder =
        NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build();
    ((NimbusJwtDecoder) refreshJwtDecoder)
        .setJwtValidator(
            new org.springframework.security.oauth2.jwt.JwtIssuerValidator("identity-service"));
    jwtService = new JwtService(jwtEncoder, jwtDecoder, refreshJwtDecoder);
    ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", 7200L);
    ReflectionTestUtils.setField(jwtService, "refreshableDuration", 604800L);

    accountRepository = mock(AccountRepository.class);
    invalidatedTokenRepository = mock(InvalidatedTokenRepository.class);
    passwordEncoder = new BCryptPasswordEncoder();
    accountMapper = mock(AccountMapper.class);

    accountService = new AccountService(accountRepository, accountMapper, passwordEncoder);
    authenticationService =
        new AuthenticationService(
            accountRepository,
            invalidatedTokenRepository,
            passwordEncoder,
            jwtService,
            mock(AccountMapper.class));
  }

  @Test
  void meReturnsAuthoritativeDatabaseEmailAndRoleInsteadOfJwtClaims() {
    UUID accountId = UUID.randomUUID();
    Account account = buildAccount(accountId, "db-authoritative@example.com", Role.RECRUITER, 4L);
    String token =
        issueToken(
            "authoritative-me-jti",
            accountId.toString(),
            "claim-email@example.com",
            Role.CANDIDATE.name(),
            4L);

    when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

    MeResponse response = accountService.getCurrentAccount(decodeAccessToken(token));

    assertEquals(accountId.toString(), response.accountId());
    assertEquals("db-authoritative@example.com", response.email());
    assertEquals(Role.RECRUITER.name(), response.role());
  }

  @Test
  void meRejectsTokenVersionMismatch() {
    UUID accountId = UUID.randomUUID();
    Account account = buildAccount(accountId, "me-version@example.com", Role.CANDIDATE, 2L);
    String token =
        issueToken(
            "me-version-mismatch-jti",
            accountId.toString(),
            "me-version@example.com",
            Role.CANDIDATE.name(),
            1L);
    when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

    AppException exception =
        assertThrows(
            AppException.class, () -> accountService.getCurrentAccount(decodeAccessToken(token)));
    assertEquals(ErrorCode.UNAUTHENTICATED, exception.getErrorCode());
  }

  @Test
  void changePasswordSuccessIncrementsTokenVersionAndOldTokenFailsIntrospectionAndRefresh() {
    UUID accountId = UUID.randomUUID();
    Account account = buildAccount(accountId, "change-pass@example.com", Role.CANDIDATE, 0L);
    String oldToken = jwtService.generateAccessToken(account);
    Jwt oldJwt = decodeAccessToken(oldToken);

    when(accountRepository.findById(accountId)).thenAnswer(invocation -> Optional.of(account));
    when(accountRepository.save(any(Account.class)))
        .thenAnswer(
            invocation -> {
              account.setTokenVersion(invocation.getArgument(0, Account.class).getTokenVersion());
              account.setPasswordHash(invocation.getArgument(0, Account.class).getPasswordHash());
              return invocation.getArgument(0);
            });
    when(invalidatedTokenRepository.existsById(oldJwt.getId())).thenReturn(false);

    accountService.changePassword(oldJwt, new ChangePasswordRequest("secret123", "new-secret-123"));

    assertEquals(1L, account.getTokenVersion());
    assertTrue(passwordEncoder.matches("new-secret-123", account.getPasswordHash()));

    IntrospectResponse introspectResponse =
        authenticationService.introspect(new IntrospectRequest(oldToken));
    assertFalse(introspectResponse.valid());

    AppException refreshException =
        assertThrows(
            AppException.class, () -> authenticationService.refresh(new RefreshRequest(oldToken)));
    assertEquals(ErrorCode.UNAUTHENTICATED, refreshException.getErrorCode());
    verify(invalidatedTokenRepository, never()).saveAndFlush(any());
  }

  @Test
  void changePasswordWrongCurrentPasswordReturnsIdentityBadRequestError() {
    UUID accountId = UUID.randomUUID();
    Account account = buildAccount(accountId, "wrong-current@example.com", Role.CANDIDATE, 0L);
    String token = jwtService.generateAccessToken(account);

    when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

    AppException exception =
        assertThrows(
            AppException.class,
            () ->
                accountService.changePassword(
                    decodeAccessToken(token),
                    new ChangePasswordRequest("bad-current", "new-secret-123")));

    assertEquals(ErrorCode.INVALID_CURRENT_PASSWORD, exception.getErrorCode());
    assertEquals(0L, account.getTokenVersion());
    verify(accountRepository, never()).save(any(Account.class));
  }

  @Test
  void getAccountsReturnsPagedFilteredResponseForAdmin() {
    UUID accountId = UUID.randomUUID();
    Account account = buildAccount(accountId, "filter@example.com", Role.CANDIDATE, 0L);
    AccountResponse mapped =
        new AccountResponse(
            accountId, "filter@example.com", Role.CANDIDATE, true, account.getCreatedAt());
    when(accountRepository.findAll(
            any(org.springframework.data.jpa.domain.Specification.class), any(PageRequest.class)))
        .thenReturn(new PageImpl<>(java.util.List.of(account), PageRequest.of(0, 20), 1));
    when(accountMapper.toAccountResponse(account)).thenReturn(mapped);

    PageResponse<AccountResponse> response =
        accountService.getAccounts("filter", "CANDIDATE", true, 0, 20);

    assertEquals(1, response.content().size());
    assertEquals(accountId, response.content().getFirst().id());
    assertEquals(0, response.page());
    assertEquals(20, response.size());
    assertEquals(1L, response.totalElements());
  }

  @Test
  void updateAccountEnabledNoOpWhenSameStateDoesNotIncrementTokenVersion() {
    UUID accountId = UUID.randomUUID();
    Account account = buildAccount(accountId, "enabled@example.com", Role.CANDIDATE, 2L);
    when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
    when(accountMapper.toAccountResponse(account))
        .thenReturn(
            new AccountResponse(
                accountId, account.getEmail(), account.getRole(), true, account.getCreatedAt()));

    Jwt adminJwt =
        Jwt.withTokenValue("token")
            .subject(UUID.randomUUID().toString())
            .header("alg", "none")
            .build();

    accountService.updateAccountEnabled(accountId, true, adminJwt);

    assertEquals(2L, account.getTokenVersion());
    verify(accountRepository, never()).save(any(Account.class));
  }

  @Test
  void updateAccountEnabledRealChangeIncrementsTokenVersionExactlyOnce() {
    UUID accountId = UUID.randomUUID();
    Account account = buildAccount(accountId, "enabled@example.com", Role.CANDIDATE, 5L);
    account.setEnabled(true);
    when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
    when(accountRepository.save(any(Account.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(accountMapper.toAccountResponse(any(Account.class)))
        .thenAnswer(
            invocation -> {
              Account mapped = invocation.getArgument(0);
              return new AccountResponse(
                  mapped.getId(),
                  mapped.getEmail(),
                  mapped.getRole(),
                  mapped.isEnabled(),
                  mapped.getCreatedAt());
            });

    Jwt adminJwt =
        Jwt.withTokenValue("token")
            .subject(UUID.randomUUID().toString())
            .header("alg", "none")
            .build();

    AccountResponse response = accountService.updateAccountEnabled(accountId, false, adminJwt);

    assertFalse(response.enabled());
    assertEquals(6L, account.getTokenVersion());
    verify(accountRepository, times(1)).save(any(Account.class));
  }

  @Test
  void updateAccountEnabledCannotDisableCurrentAdmin() {
    UUID adminId = UUID.randomUUID();
    Jwt adminJwt =
        Jwt.withTokenValue("token").subject(adminId.toString()).header("alg", "none").build();

    AppException exception =
        assertThrows(
            AppException.class,
            () -> accountService.updateAccountEnabled(adminId, false, adminJwt));

    assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
    verify(accountRepository, never()).findById(any());
  }

  @Test
  void getAdminStatisticsCountsByRoleAndEnabled() {
    when(accountRepository.count()).thenReturn(10L);
    when(accountRepository.countByRole(Role.CANDIDATE)).thenReturn(4L);
    when(accountRepository.countByRole(Role.RECRUITER)).thenReturn(3L);
    when(accountRepository.countByRole(Role.ADMIN)).thenReturn(3L);
    when(accountRepository.countByEnabled(true)).thenReturn(7L);

    AdminAccountStatisticsResponse response = accountService.getAdminStatistics();

    assertEquals(10L, response.totalAccounts());
    assertEquals(4L, response.candidateAccounts());
    assertEquals(3L, response.recruiterAccounts());
    assertEquals(3L, response.adminAccounts());
    assertEquals(7L, response.enabledAccounts());
    assertEquals(3L, response.disabledAccounts());
  }

  private Account buildAccount(UUID accountId, String email, Role role, long tokenVersion) {
    return Account.builder()
        .id(accountId)
        .email(email)
        .passwordHash(passwordEncoder.encode("secret123"))
        .role(role)
        .enabled(true)
        .tokenVersion(tokenVersion)
        .createdAt(Instant.now())
        .build();
  }

  private Jwt decodeAccessToken(String token) {
    return jwtService.decodeToken(token).orElseThrow();
  }

  private String issueToken(
      String jti, String subject, String emailClaim, String roleClaim, long tokenVersionClaim) {
    Instant now = Instant.now();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .id(jti)
            .issuer("identity-service")
            .issuedAt(now)
            .expiresAt(now.plus(1, ChronoUnit.HOURS))
            .subject(subject)
            .claim("email", emailClaim)
            .claim("role", roleClaim)
            .claim("tokenVersion", tokenVersionClaim)
            .build();

    return jwtEncoder
        .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
        .getTokenValue();
  }
}
