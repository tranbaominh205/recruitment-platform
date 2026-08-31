package com.tbm.recruitment.identity.configuration;

import com.tbm.recruitment.identity.exception.ErrorCode;
import com.tbm.recruitment.identity.security.SecurityResponseWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfiguration {

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http, JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {

    http.csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(
            requests ->
                requests
                    .requestMatchers(
                        "/identity/health",
                        "/identity/auth/register",
                        "/identity/auth/login",
                        "/identity/auth/introspect",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html")
                    .permitAll()
                    .requestMatchers("/identity/admin/**")
                    .hasRole("ADMIN")
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(
            oauth2 ->
                oauth2
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                    .authenticationEntryPoint(
                        (request, response, exception) ->
                            SecurityResponseWriter.write(response, ErrorCode.UNAUTHENTICATED)))
        .exceptionHandling(
            exceptions ->
                exceptions
                    .authenticationEntryPoint(
                        (request, response, exception) ->
                            SecurityResponseWriter.write(response, ErrorCode.UNAUTHENTICATED))
                    .accessDeniedHandler(
                        (request, response, exception) ->
                            SecurityResponseWriter.write(response, ErrorCode.UNAUTHORIZED)));

    return http.build();
  }

  @Bean
  JwtAuthenticationConverter jwtAuthenticationConverter() {

    JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();

    authoritiesConverter.setAuthoritiesClaimName("role");
    authoritiesConverter.setAuthorityPrefix("ROLE_");

    JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();

    authenticationConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);

    return authenticationConverter;
  }
}
