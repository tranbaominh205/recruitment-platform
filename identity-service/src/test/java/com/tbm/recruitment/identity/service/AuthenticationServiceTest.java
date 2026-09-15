package com.tbm.recruitment.identity.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tbm.recruitment.identity.dto.request.IntrospectRequest;
import com.tbm.recruitment.identity.dto.request.LoginRequest;
import com.tbm.recruitment.identity.dto.request.RefreshRequest;
import com.tbm.recruitment.identity.dto.response.IntrospectResponse;
import com.tbm.recruitment.identity.dto.response.LoginResponse;
import com.tbm.recruitment.identity.entity.Account;
import com.tbm.recruitment.identity.entity.AdminPermission;
import com.tbm.recruitment.identity.entity.InvalidatedToken;
import com.tbm.recruitment.identity.entity.Role;
import com.tbm.recruitment.identity.exception.AppException;
import com.tbm.recruitment.identity.exception.ErrorCode;
import com.tbm.recruitment.identity.mapper.AccountMapper;
import com.tbm.recruitment.identity.repository.AccountRepository;
import com.tbm.recruitment.identity.repository.InvalidatedTokenRepository;
import com.tbm.recruitment.identity.security.JwtService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.dao.DataIntegrityViolationException;
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

class AuthenticationServiceTest {

  private AuthenticationService authenticationService;
  private AccountRepository accountRepository;
  private InvalidatedTokenRepository invalidatedTokenRepository;
  private JwtService jwtService;
  private PasswordEncoder passwordEncoder;
  private JwtEncoder jwtEncoder;

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

    jwtService = spy(new JwtService(jwtEncoder, jwtDecoder, refreshJwtDecoder));
    ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", 7200L);
    ReflectionTestUtils.setField(jwtService, "refreshableDuration", 604800L);

    accountRepository = mock(AccountRepository.class);
    invalidatedTokenRepository = mock(InvalidatedTokenRepository.class);
    passwordEncoder = new BCryptPasswordEncoder();
    authenticationService =
        new AuthenticationService(
            accountRepository,
            invalidatedTokenRepository,
            passwordEncoder,
            jwtService,
            mock(AccountMapper.class));
  }

  @Test
  void loginExpiresInMatchesConfiguredAccessTokenExpiration() {
    Account account =
        buildAccount(UUID.randomUUID(), "candidate@example.com", Role.CANDIDATE, true, 0L);

    when(accountRepository.findByEmailIgnoreCase("candidate@example.com"))
        .thenReturn(Optional.of(account));

    LoginResponse response =
        authenticationService.login(new LoginRequest("candidate@example.com", "secret123"));

    assertEquals("Bearer", response.tokenType());
    assertEquals(7200L, response.expiresIn());
    assertNotNull(response.accessToken());
  }

  @Test
  void introspectReturnsAccountValuesForValidEnabledAccount() {
    UUID accountId = UUID.randomUUID();
    Account account = buildAccount(accountId, "db-account@example.com", Role.RECRUITER, true, 0L);
    String token =
        issueToken(
            "valid-account-jti",
            accountId.toString(),
            Instant.now().minusSeconds(60),
            Instant.now().plusSeconds(300),
            "identity-service");

    when(invalidatedTokenRepository.existsById("valid-account-jti")).thenReturn(false);
    when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

    IntrospectResponse response = authenticationService.introspect(new IntrospectRequest(token));

    assertTrue(response.valid());
    assertEquals(account.getId().toString(), response.accountId());
    assertEquals(account.getEmail(), response.email());
    assertEquals(account.getRole().name(), response.role());
  }

  @Test
  void introspectReturnsAdminPermissionsFromDatabaseStateAndEmptyForCandidate() {
    UUID candidateId = UUID.randomUUID();
    Account candidate =
        buildAccount(candidateId, "candidate@example.com", Role.CANDIDATE, true, 0L);
    String candidateToken =
        issueToken(
            "candidate-introspect-jti",
            candidateId.toString(),
            Instant.now().minusSeconds(60),
            Instant.now().plusSeconds(300),
            "identity-service");
    when(invalidatedTokenRepository.existsById("candidate-introspect-jti")).thenReturn(false);
    when(accountRepository.findById(candidateId)).thenReturn(Optional.of(candidate));

    IntrospectResponse candidateResponse =
        authenticationService.introspect(new IntrospectRequest(candidateToken));
    assertTrue(candidateResponse.valid());
    assertEquals(java.util.List.of(), candidateResponse.permissions());

    UUID adminId = UUID.randomUUID();
    Account admin = buildAccount(adminId, "admin@example.com", Role.ADMIN, true, 0L);
    admin.setAdminPermissions(
        java.util.EnumSet.of(
            AdminPermission.ACCOUNT_DISABLE, AdminPermission.ACCOUNT_REVOKE_SESSIONS));
    String adminToken =
        issueToken(
            "admin-introspect-jti",
            adminId.toString(),
            Instant.now().minusSeconds(60),
            Instant.now().plusSeconds(300),
            "identity-service");
    when(invalidatedTokenRepository.existsById("admin-introspect-jti")).thenReturn(false);
    when(accountRepository.findById(adminId)).thenReturn(Optional.of(admin));

    IntrospectResponse adminResponse =
        authenticationService.introspect(new IntrospectRequest(adminToken));
    assertTrue(adminResponse.valid());
    assertEquals(
        java.util.List.of("ACCOUNT_DISABLE", "ACCOUNT_REVOKE_SESSIONS"),
        adminResponse.permissions());
  }

  @Test
  void introspectReturnsFalseWhenTokenJtiIsRevoked() {
    String token =
        issueToken(
            "revoked-jti",
            UUID.randomUUID().toString(),
            Instant.now().minusSeconds(60),
            Instant.now().plusSeconds(300),
            "identity-service");

    when(invalidatedTokenRepository.existsById("revoked-jti")).thenReturn(true);

    IntrospectResponse response = authenticationService.introspect(new IntrospectRequest(token));

    assertFalse(response.valid());
    assertEquals(null, response.accountId());
    assertEquals(null, response.email());
    assertEquals(null, response.role());
  }

  @Test
  void refreshValidCurrentTokenInvalidatesOldJtiAndReturnsNewTokenWithDifferentJti() {
    UUID accountId = UUID.randomUUID();
    Instant issuedAt = Instant.now().minusSeconds(120);
    Instant expiresAt = Instant.now().plusSeconds(1800);
    String oldToken =
        issueToken("refresh-jti-1", accountId.toString(), issuedAt, expiresAt, "identity-service");
    Account account = buildAccount(accountId, "refresh@example.com", Role.CANDIDATE, true, 0L);

    when(invalidatedTokenRepository.existsById("refresh-jti-1")).thenReturn(false);
    when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
    when(invalidatedTokenRepository.saveAndFlush(any(InvalidatedToken.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    LoginResponse response = authenticationService.refresh(new RefreshRequest(oldToken));

    verify(invalidatedTokenRepository)
        .saveAndFlush(
            argThat(
                token ->
                    token.getId().equals("refresh-jti-1")
                        && token.getExpiresAt().getEpochSecond()
                            == issuedAt
                                .plusSeconds(jwtService.getRefreshableDurationSeconds())
                                .getEpochSecond()));
    InOrder inOrder = inOrder(invalidatedTokenRepository, jwtService);
    inOrder.verify(invalidatedTokenRepository).saveAndFlush(any(InvalidatedToken.class));
    inOrder.verify(jwtService).generateAccessToken(account);

    assertEquals("Bearer", response.tokenType());
    assertEquals(7200L, response.expiresIn());
    assertNotNull(response.accessToken());

    Jwt refreshedJwt = jwtService.decodeToken(response.accessToken()).orElseThrow();
    assertNotEquals("refresh-jti-1", refreshedJwt.getId());
  }

  @Test
  void refreshExpiredAccessTokenInsideRefreshWindowSucceeds() {
    UUID accountId = UUID.randomUUID();
    Instant issuedAt = Instant.now().minus(2, ChronoUnit.HOURS);
    Instant expiresAt = Instant.now().minus(1, ChronoUnit.HOURS);
    String token =
        issueToken(
            "refresh-expired-jti", accountId.toString(), issuedAt, expiresAt, "identity-service");
    Account account =
        buildAccount(accountId, "expired-refresh@example.com", Role.CANDIDATE, true, 0L);

    when(invalidatedTokenRepository.existsById("refresh-expired-jti")).thenReturn(false);
    when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
    when(invalidatedTokenRepository.saveAndFlush(any(InvalidatedToken.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    LoginResponse response = authenticationService.refresh(new RefreshRequest(token));

    assertNotNull(response.accessToken());
  }

  @Test
  void refreshFailsWhenTokenAlreadyInvalidated() {
    String token =
        issueToken(
            "already-invalidated-jti",
            UUID.randomUUID().toString(),
            Instant.now().minusSeconds(60),
            Instant.now().plusSeconds(300),
            "identity-service");

    when(invalidatedTokenRepository.existsById("already-invalidated-jti")).thenReturn(true);

    AppException exception =
        assertThrows(
            AppException.class, () -> authenticationService.refresh(new RefreshRequest(token)));
    assertEquals(ErrorCode.UNAUTHENTICATED, exception.getErrorCode());
  }

  @Test
  void sameTokenCannotRefreshTwice() {
    UUID accountId = UUID.randomUUID();
    String token =
        issueToken(
            "one-time-jti",
            accountId.toString(),
            Instant.now().minusSeconds(60),
            Instant.now().plusSeconds(300),
            "identity-service");
    Account account = buildAccount(accountId, "one-time@example.com", Role.CANDIDATE, true, 0L);
    Set<String> invalidated = new HashSet<>();

    when(invalidatedTokenRepository.existsById(anyString()))
        .thenAnswer(invocation -> invalidated.contains(invocation.getArgument(0)));
    when(invalidatedTokenRepository.saveAndFlush(any(InvalidatedToken.class)))
        .thenAnswer(
            invocation -> {
              InvalidatedToken saved = invocation.getArgument(0);
              invalidated.add(saved.getId());
              return saved;
            });
    when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

    LoginResponse first = authenticationService.refresh(new RefreshRequest(token));
    assertNotNull(first.accessToken());

    AppException second =
        assertThrows(
            AppException.class, () -> authenticationService.refresh(new RefreshRequest(token)));
    assertEquals(ErrorCode.UNAUTHENTICATED, second.getErrorCode());
  }

  @Test
  void refreshFailsWhenAccountMissing() {
    UUID accountId = UUID.randomUUID();
    String token =
        issueToken(
            "missing-account-jti",
            accountId.toString(),
            Instant.now().minusSeconds(60),
            Instant.now().plusSeconds(300),
            "identity-service");

    when(invalidatedTokenRepository.existsById("missing-account-jti")).thenReturn(false);
    when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

    AppException exception =
        assertThrows(
            AppException.class, () -> authenticationService.refresh(new RefreshRequest(token)));
    assertEquals(ErrorCode.UNAUTHENTICATED, exception.getErrorCode());
  }

  @Test
  void refreshFailsWhenAccountDisabled() {
    UUID accountId = UUID.randomUUID();
    String token =
        issueToken(
            "disabled-account-jti",
            accountId.toString(),
            Instant.now().minusSeconds(60),
            Instant.now().plusSeconds(300),
            "identity-service");
    Account disabledAccount =
        buildAccount(accountId, "disabled@example.com", Role.CANDIDATE, false, 0L);

    when(invalidatedTokenRepository.existsById("disabled-account-jti")).thenReturn(false);
    when(accountRepository.findById(accountId)).thenReturn(Optional.of(disabledAccount));

    AppException exception =
        assertThrows(
            AppException.class, () -> authenticationService.refresh(new RefreshRequest(token)));
    assertEquals(ErrorCode.UNAUTHENTICATED, exception.getErrorCode());
  }

  @Test
  void refreshFailsForMalformedSubject() {
    String token =
        issueToken(
            "bad-subject-jti",
            "not-a-uuid",
            Instant.now().minusSeconds(60),
            Instant.now().plusSeconds(300),
            "identity-service");

    AppException exception =
        assertThrows(
            AppException.class, () -> authenticationService.refresh(new RefreshRequest(token)));
    assertEquals(ErrorCode.UNAUTHENTICATED, exception.getErrorCode());
  }

  @Test
  void refreshFailsWhenJtiMissing() {
    String token =
        issueToken(
            null,
            UUID.randomUUID().toString(),
            Instant.now().minusSeconds(60),
            Instant.now().plusSeconds(300),
            "identity-service");

    AppException exception =
        assertThrows(
            AppException.class, () -> authenticationService.refresh(new RefreshRequest(token)));
    assertEquals(ErrorCode.UNAUTHENTICATED, exception.getErrorCode());
  }

  @Test
  void refreshFailsWhenIssuedAtMissing() {
    String token =
        issueToken(
            "missing-iat-jti",
            UUID.randomUUID().toString(),
            null,
            Instant.now().plusSeconds(300),
            "identity-service");

    AppException exception =
        assertThrows(
            AppException.class, () -> authenticationService.refresh(new RefreshRequest(token)));
    assertEquals(ErrorCode.UNAUTHENTICATED, exception.getErrorCode());
  }

  @Test
  void refreshFailsWhenExpiresAtMissing() {
    String token =
        issueToken(
            "missing-exp-jti",
            UUID.randomUUID().toString(),
            Instant.now().minusSeconds(60),
            null,
            "identity-service");

    AppException exception =
        assertThrows(
            AppException.class, () -> authenticationService.refresh(new RefreshRequest(token)));
    assertEquals(ErrorCode.UNAUTHENTICATED, exception.getErrorCode());
  }

  @Test
  void refreshFailsWhenRefreshWindowExpired() {
    UUID accountId = UUID.randomUUID();
    Instant issuedAt = Instant.now().minusSeconds(jwtService.getRefreshableDurationSeconds() + 1);
    String token =
        issueToken(
            "expired-window-jti",
            accountId.toString(),
            issuedAt,
            Instant.now().minusSeconds(10),
            "identity-service");

    AppException exception =
        assertThrows(
            AppException.class, () -> authenticationService.refresh(new RefreshRequest(token)));
    assertEquals(ErrorCode.UNAUTHENTICATED, exception.getErrorCode());
  }

  @Test
  void refreshFailsWhenConcurrentSaveCreatesDuplicateRevocation() {
    UUID accountId = UUID.randomUUID();
    String token =
        issueToken(
            "duplicate-jti",
            accountId.toString(),
            Instant.now().minusSeconds(60),
            Instant.now().plusSeconds(300),
            "identity-service");
    Account account = buildAccount(accountId, "duplicate@example.com", Role.CANDIDATE, true, 0L);

    when(invalidatedTokenRepository.existsById("duplicate-jti")).thenReturn(false);
    when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
    when(invalidatedTokenRepository.saveAndFlush(any(InvalidatedToken.class)))
        .thenThrow(new DataIntegrityViolationException("duplicate"));

    AppException exception =
        assertThrows(
            AppException.class, () -> authenticationService.refresh(new RefreshRequest(token)));
    assertEquals(ErrorCode.UNAUTHENTICATED, exception.getErrorCode());
    verify(jwtService, never()).generateAccessToken(any(Account.class));
  }

  @Test
  void logoutStoresRevocationUntilRefreshableUntilInsteadOfAccessTokenExpiration() {
    Instant issuedAt = Instant.parse("2030-01-01T00:00:00Z");
    Instant tokenExpiresAt = issuedAt.plusSeconds(300);
    String token =
        issueToken(
            "logout-jti",
            UUID.randomUUID().toString(),
            issuedAt,
            tokenExpiresAt,
            "identity-service");

    when(invalidatedTokenRepository.existsById("logout-jti")).thenReturn(false);

    authenticationService.logout("Bearer " + token);

    verify(invalidatedTokenRepository)
        .save(
            argThat(
                invalidatedToken ->
                    invalidatedToken.getId().equals("logout-jti")
                        && invalidatedToken
                            .getExpiresAt()
                            .equals(
                                issuedAt.plusSeconds(jwtService.getRefreshableDurationSeconds()))
                        && !invalidatedToken.getExpiresAt().equals(tokenExpiresAt)));
  }

  @Test
  void expiredAccessTokenInsideRefreshWindowCanLogoutSuccessfully() {
    Instant issuedAt = Instant.now().minus(2, ChronoUnit.HOURS);
    String token =
        issueToken(
            "logout-refreshable-expired-jti",
            UUID.randomUUID().toString(),
            issuedAt,
            Instant.now().minus(1, ChronoUnit.HOURS),
            "identity-service");

    when(invalidatedTokenRepository.existsById("logout-refreshable-expired-jti")).thenReturn(false);

    authenticationService.logout("Bearer " + token);

    verify(invalidatedTokenRepository)
        .save(
            argThat(
                invalidatedToken ->
                    invalidatedToken.getId().equals("logout-refreshable-expired-jti")
                        && invalidatedToken.getExpiresAt().getEpochSecond()
                            == issuedAt
                                .plusSeconds(jwtService.getRefreshableDurationSeconds())
                                .getEpochSecond()));
  }

  @Test
  void logoutFailsWhenRefreshWindowExpired() {
    Instant issuedAt = Instant.now().minusSeconds(jwtService.getRefreshableDurationSeconds() + 1);
    String token =
        issueToken(
            "logout-expired-window-jti",
            UUID.randomUUID().toString(),
            issuedAt,
            Instant.now().minusSeconds(10),
            "identity-service");

    AppException exception =
        assertThrows(AppException.class, () -> authenticationService.logout("Bearer " + token));
    assertEquals(ErrorCode.UNAUTHENTICATED, exception.getErrorCode());
    verify(invalidatedTokenRepository, never()).save(any(InvalidatedToken.class));
  }

  @Test
  void tokenLoggedOutCannotRefresh() {
    UUID accountId = UUID.randomUUID();
    Instant issuedAt = Instant.now().minusSeconds(120);
    String token =
        issueToken(
            "logout-refresh-jti",
            accountId.toString(),
            issuedAt,
            Instant.now().plusSeconds(300),
            "identity-service");

    when(invalidatedTokenRepository.existsById("logout-refresh-jti")).thenReturn(false, true);
    when(invalidatedTokenRepository.save(any(InvalidatedToken.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    authenticationService.logout("Bearer " + token);

    AppException exception =
        assertThrows(
            AppException.class, () -> authenticationService.refresh(new RefreshRequest(token)));
    assertEquals(ErrorCode.UNAUTHENTICATED, exception.getErrorCode());
  }

  @Test
  void repeatedDirectLogoutIsIdempotentEvenWithConcurrentInsertRace() {
    String token =
        issueToken(
            "repeat-logout-jti",
            UUID.randomUUID().toString(),
            Instant.parse("2030-01-02T00:00:00Z"),
            Instant.parse("2030-01-02T00:05:00Z"),
            "identity-service");

    when(invalidatedTokenRepository.existsById("repeat-logout-jti")).thenReturn(false, true);
    when(invalidatedTokenRepository.save(any(InvalidatedToken.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    authenticationService.logout("Bearer " + token);
    authenticationService.logout("Bearer " + token);

    verify(invalidatedTokenRepository, times(1))
        .save(argThat(invalidatedToken -> invalidatedToken.getId().equals("repeat-logout-jti")));
  }

  @Test
  void logoutRejectsMissingOrMalformedToken() {
    AppException missingHeaderException =
        assertThrows(AppException.class, () -> authenticationService.logout(null));
    assertEquals(ErrorCode.UNAUTHENTICATED, missingHeaderException.getErrorCode());

    AppException malformedTokenException =
        assertThrows(AppException.class, () -> authenticationService.logout("invalid"));
    assertEquals(ErrorCode.UNAUTHENTICATED, malformedTokenException.getErrorCode());
  }

  @Test
  void introspectReturnsFalseForMalformedOrInvalidToken() {
    IntrospectResponse response =
        authenticationService.introspect(new IntrospectRequest("this-is-not-valid-jwt"));

    assertFalse(response.valid());
  }

  @Test
  void introspectReturnsFalseForExpiredToken() {
    String token =
        issueToken(
            "expired-introspect-jti",
            UUID.randomUUID().toString(),
            Instant.now().minus(2, ChronoUnit.HOURS),
            Instant.now().minus(1, ChronoUnit.HOURS),
            "identity-service");

    IntrospectResponse response = authenticationService.introspect(new IntrospectRequest(token));

    assertFalse(response.valid());
  }

  @Test
  void introspectReturnsFalseWhenTokenVersionMissingNonNumericOrMismatched() {
    UUID accountId = UUID.randomUUID();
    Account account = buildAccount(accountId, "versioned@example.com", Role.CANDIDATE, true, 3L);

    String missingTokenVersionClaimToken =
        issueTokenWithoutTokenVersion(
            "missing-token-version-jti",
            accountId.toString(),
            Instant.now().minusSeconds(30),
            Instant.now().plusSeconds(300),
            "identity-service");

    when(invalidatedTokenRepository.existsById("missing-token-version-jti")).thenReturn(false);
    when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

    IntrospectResponse missingClaimResponse =
        authenticationService.introspect(new IntrospectRequest(missingTokenVersionClaimToken));
    assertFalse(missingClaimResponse.valid());

    String nonNumericTokenVersionClaimToken =
        issueTokenWithTokenVersion(
            "non-numeric-token-version-jti",
            accountId.toString(),
            Instant.now().minusSeconds(30),
            Instant.now().plusSeconds(300),
            "identity-service",
            "not-a-number");

    when(invalidatedTokenRepository.existsById("non-numeric-token-version-jti")).thenReturn(false);

    IntrospectResponse nonNumericClaimResponse =
        authenticationService.introspect(new IntrospectRequest(nonNumericTokenVersionClaimToken));
    assertFalse(nonNumericClaimResponse.valid());

    String mismatchTokenVersionClaimToken =
        issueTokenWithTokenVersion(
            "mismatch-token-version-jti",
            accountId.toString(),
            Instant.now().minusSeconds(30),
            Instant.now().plusSeconds(300),
            "identity-service",
            1L);

    when(invalidatedTokenRepository.existsById("mismatch-token-version-jti")).thenReturn(false);

    IntrospectResponse mismatchClaimResponse =
        authenticationService.introspect(new IntrospectRequest(mismatchTokenVersionClaimToken));
    assertFalse(mismatchClaimResponse.valid());
  }

  @Test
  void refreshFailsForTokenVersionMismatchBeforeRevocationOrNewTokenIssue() {
    UUID accountId = UUID.randomUUID();
    Account account =
        buildAccount(accountId, "refresh-version@example.com", Role.CANDIDATE, true, 2L);
    String token =
        issueTokenWithTokenVersion(
            "refresh-version-mismatch-jti",
            accountId.toString(),
            Instant.now().minusSeconds(60),
            Instant.now().plusSeconds(300),
            "identity-service",
            1L);

    when(invalidatedTokenRepository.existsById("refresh-version-mismatch-jti")).thenReturn(false);
    when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

    AppException exception =
        assertThrows(
            AppException.class, () -> authenticationService.refresh(new RefreshRequest(token)));
    assertEquals(ErrorCode.UNAUTHENTICATED, exception.getErrorCode());
    verify(invalidatedTokenRepository, never()).saveAndFlush(any(InvalidatedToken.class));
    verify(jwtService, never()).generateAccessToken(any(Account.class));
  }

  private Account buildAccount(
      UUID accountId, String email, Role role, boolean enabled, long tokenVersion) {
    Account account =
        Account.builder()
            .id(accountId)
            .email(email)
            .passwordHash(passwordEncoder.encode("secret123"))
            .role(role)
            .enabled(enabled)
            .tokenVersion(tokenVersion)
            .createdAt(Instant.now())
            .build();
    if (role == Role.ADMIN) {
      account.setAdminPermissions(java.util.EnumSet.allOf(AdminPermission.class));
    }
    return account;
  }

  private String issueToken(
      String jti, String subject, Instant issuedAt, Instant expiresAt, String issuer) {
    return issueTokenWithTokenVersion(jti, subject, issuedAt, expiresAt, issuer, 0L);
  }

  private String issueTokenWithoutTokenVersion(
      String jti, String subject, Instant issuedAt, Instant expiresAt, String issuer) {
    JwtClaimsSet.Builder claimsBuilder =
        JwtClaimsSet.builder()
            .subject(subject)
            .issuer(issuer)
            .claim("email", "token-claim@example.com")
            .claim("role", Role.CANDIDATE.name());

    if (jti != null) {
      claimsBuilder.id(jti);
    }

    if (issuedAt != null) {
      claimsBuilder.issuedAt(issuedAt);
    }

    if (expiresAt != null) {
      claimsBuilder.expiresAt(expiresAt);
    }

    return jwtEncoder
        .encode(
            JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claimsBuilder.build()))
        .getTokenValue();
  }

  private String issueTokenWithTokenVersion(
      String jti,
      String subject,
      Instant issuedAt,
      Instant expiresAt,
      String issuer,
      Object tokenVersionClaim) {
    JwtClaimsSet.Builder claimsBuilder =
        JwtClaimsSet.builder()
            .subject(subject)
            .issuer(issuer)
            .claim("email", "token-claim@example.com")
            .claim("role", Role.CANDIDATE.name())
            .claim("tokenVersion", tokenVersionClaim);

    if (jti != null) {
      claimsBuilder.id(jti);
    }

    if (issuedAt != null) {
      claimsBuilder.issuedAt(issuedAt);
    }

    if (expiresAt != null) {
      claimsBuilder.expiresAt(expiresAt);
    }

    return jwtEncoder
        .encode(
            JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claimsBuilder.build()))
        .getTokenValue();
  }
}
