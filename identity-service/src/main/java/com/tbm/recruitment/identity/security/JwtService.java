package com.tbm.recruitment.identity.security;

import com.tbm.recruitment.identity.entity.Account;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtService {

  private final JwtEncoder jwtEncoder;
  private final JwtDecoder jwtDecoder;

  @Value("${security.jwt.access-token-expiration}")
  private long accessTokenExpiration;

  public String generateAccessToken(Account account) {

    Instant now = Instant.now();

    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .id(UUID.randomUUID().toString())
            .issuer("identity-service")
            .issuedAt(now)
            .expiresAt(now.plus(accessTokenExpiration, ChronoUnit.SECONDS))
            .subject(account.getId().toString())
            .claim("email", account.getEmail())
            .claim("role", account.getRole().name())
            .build();

    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

    return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
  }

  public long getAccessTokenExpirationSeconds() {
    return accessTokenExpiration;
  }

  public Optional<Jwt> decodeToken(String token) {
    if (token == null || token.isBlank()) {
      return Optional.empty();
    }

    try {
      return Optional.of(jwtDecoder.decode(token));
    } catch (Exception exception) {
      return Optional.empty();
    }
  }
}
