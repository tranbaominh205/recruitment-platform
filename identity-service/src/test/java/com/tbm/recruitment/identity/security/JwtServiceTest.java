package com.tbm.recruitment.identity.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tbm.recruitment.identity.entity.Account;
import com.tbm.recruitment.identity.entity.Role;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class JwtServiceTest {

  private JwtService jwtService;
  private JwtDecoder jwtDecoder;
  private NimbusJwtEncoder jwtEncoder;

  @BeforeEach
  void setUp() {
    SecretKey secretKey =
        new SecretKeySpec("0123456789abcdef0123456789abcdef".getBytes(), "HmacSHA256");
    jwtDecoder = NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build();
    ((NimbusJwtDecoder) jwtDecoder)
        .setJwtValidator(JwtValidators.createDefaultWithIssuer("identity-service"));
    JwtDecoder refreshJwtDecoder =
        NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build();
    ((NimbusJwtDecoder) refreshJwtDecoder)
        .setJwtValidator(
            new org.springframework.security.oauth2.jwt.JwtIssuerValidator("identity-service"));
    jwtEncoder = NimbusJwtEncoder.withSecretKey(secretKey).build();
    jwtService = new JwtService(jwtEncoder, jwtDecoder, refreshJwtDecoder);
    ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", 3600L);
    ReflectionTestUtils.setField(jwtService, "refreshableDuration", 604800L);
  }

  @Test
  void generatedAccessTokenContainsAccountClaimsAndJtiAndExpiration() {
    UUID accountId = UUID.randomUUID();
    Account account =
        Account.builder()
            .id(accountId)
            .email("candidate@example.com")
            .passwordHash("hashed")
            .role(Role.CANDIDATE)
            .enabled(true)
            .tokenVersion(2L)
            .createdAt(Instant.now())
            .build();

    String token = jwtService.generateAccessToken(account);
    Jwt jwt = jwtDecoder.decode(token);

    assertEquals(accountId.toString(), jwt.getSubject());
    assertEquals("candidate@example.com", jwt.getClaimAsString("email"));
    assertEquals(Role.CANDIDATE.name(), jwt.getClaimAsString("role"));
    assertEquals(2L, ((Number) jwt.getClaims().get("tokenVersion")).longValue());
    assertNotNull(jwt.getId());
    assertFalse(jwt.getId().isBlank());
    assertNotNull(jwt.getExpiresAt());
  }

  @Test
  void generatedTokensForSameAccountHaveDifferentJtis() {
    UUID accountId = UUID.randomUUID();
    Account account =
        Account.builder()
            .id(accountId)
            .email("candidate@example.com")
            .passwordHash("hashed")
            .role(Role.CANDIDATE)
            .enabled(true)
            .tokenVersion(0L)
            .createdAt(Instant.now())
            .build();

    String firstToken = jwtService.generateAccessToken(account);
    String secondToken = jwtService.generateAccessToken(account);

    assertFalse(
        jwtDecoder.decode(firstToken).getId().equals(jwtDecoder.decode(secondToken).getId()));
  }

  @Test
  void strictDecodeRejectsExpiredToken() {
    String expiredToken =
        jwtEncoder
            .encode(
                JwtEncoderParameters.from(
                    JwsHeader.with(MacAlgorithm.HS256).build(),
                    JwtClaimsSet.builder()
                        .id("expired-jti")
                        .issuer("identity-service")
                        .issuedAt(Instant.now().minus(2, ChronoUnit.DAYS))
                        .expiresAt(Instant.now().minus(1, ChronoUnit.DAYS))
                        .subject(UUID.randomUUID().toString())
                        .build()))
            .getTokenValue();

    assertTrue(jwtService.decodeToken(expiredToken).isEmpty());
  }

  @Test
  void strictDecodeRejectsWrongIssuer() {
    String tokenWithWrongIssuer =
        jwtEncoder
            .encode(
                JwtEncoderParameters.from(
                    JwsHeader.with(MacAlgorithm.HS256).build(),
                    JwtClaimsSet.builder()
                        .id("strict-wrong-issuer-jti")
                        .issuer("another-issuer")
                        .issuedAt(Instant.now())
                        .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                        .subject(UUID.randomUUID().toString())
                        .build()))
            .getTokenValue();

    assertTrue(jwtService.decodeToken(tokenWithWrongIssuer).isEmpty());
  }

  @Test
  void refreshDecodeAcceptsExpiredTokenWithValidSignatureAndIssuer() {
    String expiredToken =
        jwtEncoder
            .encode(
                JwtEncoderParameters.from(
                    JwsHeader.with(MacAlgorithm.HS256).build(),
                    JwtClaimsSet.builder()
                        .id("refreshable-expired-jti")
                        .issuer("identity-service")
                        .issuedAt(Instant.now().minus(2, ChronoUnit.HOURS))
                        .expiresAt(Instant.now().minus(1, ChronoUnit.HOURS))
                        .subject(UUID.randomUUID().toString())
                        .build()))
            .getTokenValue();

    assertTrue(jwtService.decodeRefreshableToken(expiredToken).isPresent());
  }

  @Test
  void refreshDecodeRejectsTokenWithBadSignature() {
    SecretKey otherKey =
        new SecretKeySpec("fedcba9876543210fedcba9876543210".getBytes(), "HmacSHA256");
    String tokenWithOtherSignature =
        NimbusJwtEncoder.withSecretKey(otherKey)
            .build()
            .encode(
                JwtEncoderParameters.from(
                    JwsHeader.with(MacAlgorithm.HS256).build(),
                    JwtClaimsSet.builder()
                        .id("bad-signature-jti")
                        .issuer("identity-service")
                        .issuedAt(Instant.now())
                        .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                        .subject(UUID.randomUUID().toString())
                        .build()))
            .getTokenValue();

    assertTrue(jwtService.decodeRefreshableToken(tokenWithOtherSignature).isEmpty());
  }

  @Test
  void refreshDecodeRejectsWrongIssuer() {
    String tokenWithWrongIssuer =
        jwtEncoder
            .encode(
                JwtEncoderParameters.from(
                    JwsHeader.with(MacAlgorithm.HS256).build(),
                    JwtClaimsSet.builder()
                        .id("wrong-issuer-jti")
                        .issuer("another-issuer")
                        .issuedAt(Instant.now())
                        .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                        .subject(UUID.randomUUID().toString())
                        .build()))
            .getTokenValue();

    assertTrue(jwtService.decodeRefreshableToken(tokenWithWrongIssuer).isEmpty());
  }
}
