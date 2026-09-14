package com.tbm.recruitment.identity.configuration;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
public class JwtConfiguration {

  @Bean
  SecretKey jwtSecretKey(@Value("${security.jwt.signer-key}") String signerKey) {
    return new SecretKeySpec(signerKey.getBytes(), "HmacSHA256");
  }

  @Bean
  JwtEncoder jwtEncoder(SecretKey secretKey) {
    return NimbusJwtEncoder.withSecretKey(secretKey).build();
  }

  @Bean
  JwtDecoder jwtDecoder(SecretKey secretKey) {
    NimbusJwtDecoder jwtDecoder =
        NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build();
    jwtDecoder.setJwtValidator(JwtValidators.createDefaultWithIssuer("identity-service"));
    return jwtDecoder;
  }

  @Bean
  @Qualifier("refreshJwtDecoder")
  JwtDecoder refreshJwtDecoder(SecretKey secretKey) {
    NimbusJwtDecoder jwtDecoder =
        NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build();
    OAuth2TokenValidator<Jwt> issuerValidator = new JwtIssuerValidator("identity-service");
    jwtDecoder.setJwtValidator(issuerValidator);
    return jwtDecoder;
  }
}
