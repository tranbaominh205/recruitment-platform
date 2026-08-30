package com.tbm.recruitment.identity.security;

import com.tbm.recruitment.identity.entity.Account;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtEncoder jwtEncoder;

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

        return jwtEncoder
                .encode(JwtEncoderParameters.from(header, claims))
                .getTokenValue();
    }
}