package com.tbm.recruitment.identity.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.tbm.recruitment.identity.entity.Account;
import com.tbm.recruitment.identity.entity.Role;
import java.time.Instant;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class JwtServiceTest {

  private JwtService jwtService;
  private JwtDecoder jwtDecoder;

  @BeforeEach
  void setUp() {
    SecretKey secretKey =
        new SecretKeySpec("0123456789abcdef0123456789abcdef".getBytes(), "HmacSHA256");
    jwtDecoder = NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build();
    jwtService = new JwtService(NimbusJwtEncoder.withSecretKey(secretKey).build(), jwtDecoder);
    ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", 3600L);
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
            .createdAt(Instant.now())
            .build();

    String token = jwtService.generateAccessToken(account);
    Jwt jwt = jwtDecoder.decode(token);

    assertEquals(accountId.toString(), jwt.getSubject());
    assertEquals("candidate@example.com", jwt.getClaimAsString("email"));
    assertEquals(Role.CANDIDATE.name(), jwt.getClaimAsString("role"));
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
            .createdAt(Instant.now())
            .build();

    String firstToken = jwtService.generateAccessToken(account);
    String secondToken = jwtService.generateAccessToken(account);

    assertFalse(
        jwtDecoder.decode(firstToken).getId().equals(jwtDecoder.decode(secondToken).getId()));
  }
}
