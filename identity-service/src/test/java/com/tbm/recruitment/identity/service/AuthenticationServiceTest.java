package com.tbm.recruitment.identity.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tbm.recruitment.identity.dto.request.IntrospectRequest;
import com.tbm.recruitment.identity.dto.request.LoginRequest;
import com.tbm.recruitment.identity.dto.response.IntrospectResponse;
import com.tbm.recruitment.identity.dto.response.LoginResponse;
import com.tbm.recruitment.identity.entity.Account;
import com.tbm.recruitment.identity.entity.InvalidatedToken;
import com.tbm.recruitment.identity.entity.Role;
import com.tbm.recruitment.identity.exception.AppException;
import com.tbm.recruitment.identity.exception.ErrorCode;
import com.tbm.recruitment.identity.mapper.AccountMapper;
import com.tbm.recruitment.identity.repository.AccountRepository;
import com.tbm.recruitment.identity.repository.InvalidatedTokenRepository;
import com.tbm.recruitment.identity.security.JwtService;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
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
    jwtService =
        new JwtService(
            jwtEncoder,
            NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build());
    ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", 7200L);

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
        Account.builder()
            .id(UUID.randomUUID())
            .email("candidate@example.com")
            .passwordHash(passwordEncoder.encode("secret123"))
            .role(Role.CANDIDATE)
            .enabled(true)
            .createdAt(Instant.now())
            .build();

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
    Account account =
        Account.builder()
            .id(accountId)
            .email("db-account@example.com")
            .passwordHash("hashed")
            .role(Role.RECRUITER)
            .enabled(true)
            .createdAt(Instant.now())
            .build();

    when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

    String token =
        jwtEncoder
            .encode(
                JwtEncoderParameters.from(
                    JwsHeader.with(MacAlgorithm.HS256).build(),
                    JwtClaimsSet.builder()
                        .id("valid-account-jti")
                        .issuer("identity-service")
                        .issuedAt(Instant.now())
                        .expiresAt(Instant.now().plusSeconds(300))
                        .subject(accountId.toString())
                        .claim("email", "stale@example.com")
                        .claim("role", "CANDIDATE")
                        .build()))
            .getTokenValue();

    when(invalidatedTokenRepository.existsById("valid-account-jti")).thenReturn(false);

    IntrospectResponse response = authenticationService.introspect(new IntrospectRequest(token));

    assertTrue(response.valid());
    assertEquals(account.getId().toString(), response.accountId());
    assertEquals(account.getEmail(), response.email());
    assertEquals(account.getRole().name(), response.role());
  }

  @Test
  void introspectReturnsFalseWhenTokenJtiIsRevoked() {
    UUID accountId = UUID.randomUUID();
    String token =
        jwtEncoder
            .encode(
                JwtEncoderParameters.from(
                    JwsHeader.with(MacAlgorithm.HS256).build(),
                    JwtClaimsSet.builder()
                        .id("revoked-jti")
                        .issuer("identity-service")
                        .issuedAt(Instant.now())
                        .expiresAt(Instant.now().plusSeconds(300))
                        .subject(accountId.toString())
                        .claim("email", "candidate@example.com")
                        .claim("role", Role.CANDIDATE.name())
                        .build()))
            .getTokenValue();

    when(invalidatedTokenRepository.existsById("revoked-jti")).thenReturn(true);

    IntrospectResponse response = authenticationService.introspect(new IntrospectRequest(token));

    assertFalse(response.valid());
    assertEquals(null, response.accountId());
    assertEquals(null, response.email());
    assertEquals(null, response.role());
  }

  @Test
  void introspectReturnsFalseWhenAccountDoesNotExist() {
    UUID accountId = UUID.randomUUID();
    String token =
        jwtEncoder
            .encode(
                JwtEncoderParameters.from(
                    JwsHeader.with(MacAlgorithm.HS256).build(),
                    JwtClaimsSet.builder()
                        .id("missing-account-jti")
                        .issuer("identity-service")
                        .issuedAt(Instant.now())
                        .expiresAt(Instant.now().plusSeconds(300))
                        .subject(accountId.toString())
                        .claim("email", "candidate@example.com")
                        .claim("role", Role.CANDIDATE.name())
                        .build()))
            .getTokenValue();

    when(invalidatedTokenRepository.existsById("missing-account-jti")).thenReturn(false);
    when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

    IntrospectResponse response = authenticationService.introspect(new IntrospectRequest(token));

    assertFalse(response.valid());
  }

  @Test
  void introspectReturnsFalseWhenAccountIsDisabled() {
    UUID accountId = UUID.randomUUID();
    Account account =
        Account.builder()
            .id(accountId)
            .email("disabled@example.com")
            .passwordHash("hashed")
            .role(Role.CANDIDATE)
            .enabled(false)
            .createdAt(Instant.now())
            .build();

    when(invalidatedTokenRepository.existsById("disabled-account-jti")).thenReturn(false);
    when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

    String token =
        jwtEncoder
            .encode(
                JwtEncoderParameters.from(
                    JwsHeader.with(MacAlgorithm.HS256).build(),
                    JwtClaimsSet.builder()
                        .id("disabled-account-jti")
                        .issuer("identity-service")
                        .issuedAt(Instant.now())
                        .expiresAt(Instant.now().plusSeconds(300))
                        .subject(accountId.toString())
                        .claim("email", account.getEmail())
                        .claim("role", account.getRole().name())
                        .build()))
            .getTokenValue();

    IntrospectResponse response = authenticationService.introspect(new IntrospectRequest(token));

    assertFalse(response.valid());
  }

  @Test
  void logoutValidTokenPersistsInvalidatedTokenWithExactJtiAndExpiry() {
    Instant expiresAt = Instant.parse("2030-01-01T00:05:00Z");
    String token =
        jwtEncoder
            .encode(
                JwtEncoderParameters.from(
                    JwsHeader.with(MacAlgorithm.HS256).build(),
                    JwtClaimsSet.builder()
                        .id("logout-jti")
                        .issuer("identity-service")
                        .issuedAt(Instant.parse("2030-01-01T00:00:00Z"))
                        .expiresAt(expiresAt)
                        .subject(UUID.randomUUID().toString())
                        .build()))
            .getTokenValue();

    when(invalidatedTokenRepository.existsById("logout-jti")).thenReturn(false);

    authenticationService.logout("Bearer " + token);

    verify(invalidatedTokenRepository)
        .save(
            argThat(
                invalidatedToken ->
                    invalidatedToken.getId().equals("logout-jti")
                        && invalidatedToken.getExpiresAt().equals(expiresAt)));

    when(invalidatedTokenRepository.existsById("logout-jti")).thenReturn(true);
    IntrospectResponse response = authenticationService.introspect(new IntrospectRequest(token));
    assertFalse(response.valid());
  }

  @Test
  void repeatedDirectLogoutIsIdempotent() {
    Instant expiresAt = Instant.parse("2030-01-02T00:05:00Z");
    String token =
        jwtEncoder
            .encode(
                JwtEncoderParameters.from(
                    JwsHeader.with(MacAlgorithm.HS256).build(),
                    JwtClaimsSet.builder()
                        .id("repeat-logout-jti")
                        .issuer("identity-service")
                        .issuedAt(Instant.parse("2030-01-02T00:00:00Z"))
                        .expiresAt(expiresAt)
                        .subject(UUID.randomUUID().toString())
                        .build()))
            .getTokenValue();

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
        assertThrows(AppException.class, () -> authenticationService.logout("Bearer not-a-jwt"));
    assertEquals(ErrorCode.UNAUTHENTICATED, malformedTokenException.getErrorCode());
  }

  @Test
  void tokenWithMissingOrBlankJtiIsRejectedForLogoutAndIntrospection() {
    UUID accountId = UUID.randomUUID();
    String token =
        jwtEncoder
            .encode(
                JwtEncoderParameters.from(
                    JwsHeader.with(MacAlgorithm.HS256).build(),
                    JwtClaimsSet.builder()
                        .id("")
                        .issuer("identity-service")
                        .issuedAt(Instant.now())
                        .expiresAt(Instant.now().plusSeconds(300))
                        .subject(accountId.toString())
                        .claim("email", "candidate@example.com")
                        .claim("role", Role.CANDIDATE.name())
                        .build()))
            .getTokenValue();

    AppException logoutException =
        assertThrows(AppException.class, () -> authenticationService.logout("Bearer " + token));
    assertEquals(ErrorCode.UNAUTHENTICATED, logoutException.getErrorCode());

    IntrospectResponse response = authenticationService.introspect(new IntrospectRequest(token));
    assertFalse(response.valid());
  }

  @Test
  void introspectReturnsFalseForMalformedOrInvalidToken() {
    IntrospectResponse response =
        authenticationService.introspect(new IntrospectRequest("this-is-not-valid-jwt"));

    assertFalse(response.valid());
  }

  @Test
  void introspectReturnsFalseForMalformedNonUuidSubject() {
    String token =
        jwtEncoder
            .encode(
                JwtEncoderParameters.from(
                    JwsHeader.with(MacAlgorithm.HS256).build(),
                    JwtClaimsSet.builder()
                        .id("non-uuid-subject-jti")
                        .issuer("identity-service")
                        .issuedAt(Instant.now())
                        .expiresAt(Instant.now().plusSeconds(300))
                        .subject("not-a-uuid")
                        .claim("email", "candidate@example.com")
                        .claim("role", Role.CANDIDATE.name())
                        .build()))
            .getTokenValue();

    when(invalidatedTokenRepository.existsById("non-uuid-subject-jti")).thenReturn(false);

    IntrospectResponse response = authenticationService.introspect(new IntrospectRequest(token));

    assertFalse(response.valid());
  }
}
