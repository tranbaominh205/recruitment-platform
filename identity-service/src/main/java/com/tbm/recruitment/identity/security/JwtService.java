package com.tbm.recruitment.identity.security;

import com.tbm.recruitment.identity.dto.response.IntrospectResponse;
import com.tbm.recruitment.identity.entity.Account;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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

  public IntrospectResponse introspect(String token) {
    try {
      Jwt jwt = jwtDecoder.decode(token);

      return new IntrospectResponse(
          true, jwt.getSubject(), jwt.getClaimAsString("email"), jwt.getClaimAsString("role"));

    } catch (JwtException exception) {
      return new IntrospectResponse(false, null, null, null);
    }
  }
}
