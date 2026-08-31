package com.tbm.recruitment.gateway.client;

import com.tbm.recruitment.gateway.dto.request.IntrospectRequest;
import com.tbm.recruitment.gateway.dto.response.IntrospectionApiResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class IdentityClient {

  WebClient identityWebClient;

  public Mono<IntrospectionApiResponse> introspect(String token) {
    return identityWebClient
        .post()
        .uri("/identity/auth/introspect")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(new IntrospectRequest(token))
        .retrieve()
        .bodyToMono(IntrospectionApiResponse.class);
  }
}
