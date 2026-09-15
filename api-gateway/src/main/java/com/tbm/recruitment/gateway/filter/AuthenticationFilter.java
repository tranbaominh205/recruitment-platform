package com.tbm.recruitment.gateway.filter;

import com.tbm.recruitment.gateway.client.IdentityClient;
import com.tbm.recruitment.gateway.dto.response.IntrospectResult;
import com.tbm.recruitment.gateway.exception.GatewayErrorCode;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationFilter implements GlobalFilter, Ordered {

  @Value("${app.api-prefix}")
  @NonFinal
  String apiPrefix;

  static Set<String> PUBLIC_GET_ENDPOINTS = Set.of("/identity/health", "/job/search");

  static Set<String> PUBLIC_POST_ENDPOINTS =
      Set.of("/identity/auth/register", "/identity/auth/login", "/identity/auth/refresh");
  static Set<String> NO_INTROSPECTION_POST_ENDPOINTS = Set.of("/identity/auth/logout");

  IdentityClient identityClient;

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

    ServerHttpRequest request = exchange.getRequest();

    if (isPublicEndpoint(request)) {
      return chain.filter(sanitizeIdentityHeaders(exchange));
    }

    String token = extractBearerToken(request);

    if (token == null) {
      return writeError(exchange.getResponse(), GatewayErrorCode.UNAUTHENTICATED);
    }

    if (shouldBypassIntrospection(request)) {
      return chain.filter(sanitizeIdentityHeaders(exchange));
    }

    return identityClient
        .introspect(token)
        .flatMap(
            response -> {
              IntrospectResult result = response.result();

              if (result == null || !result.valid()) {
                return writeError(exchange.getResponse(), GatewayErrorCode.UNAUTHENTICATED);
              }

              ServerHttpRequest authenticatedRequest =
                  request
                      .mutate()
                      .headers(
                          headers -> {
                            headers.remove("X-Account-Id");
                            headers.remove("X-Account-Email");
                            headers.remove("X-Account-Role");
                            headers.remove("X-Account-Permissions");

                            headers.set("X-Account-Id", result.accountId());
                            headers.set("X-Account-Email", result.email());
                            headers.set("X-Account-Role", result.role());
                            headers.set(
                                "X-Account-Permissions",
                                result.permissions() == null
                                    ? ""
                                    : String.join(",", result.permissions()));
                          })
                      .build();

              ServerWebExchange authenticatedExchange =
                  exchange.mutate().request(authenticatedRequest).build();

              return chain.filter(authenticatedExchange);
            })
        .onErrorResume(
            exception -> {
              log.error("Identity introspection failed: {}", exception.getMessage());

              return writeError(
                  exchange.getResponse(), GatewayErrorCode.IDENTITY_SERVICE_UNAVAILABLE);
            });
  }

  private ServerWebExchange sanitizeIdentityHeaders(ServerWebExchange exchange) {
    ServerHttpRequest sanitizedRequest =
        exchange
            .getRequest()
            .mutate()
            .headers(
                headers -> {
                  headers.remove("X-Account-Id");
                  headers.remove("X-Account-Email");
                  headers.remove("X-Account-Role");
                  headers.remove("X-Account-Permissions");
                })
            .build();

    return exchange.mutate().request(sanitizedRequest).build();
  }

  private boolean isPublicEndpoint(ServerHttpRequest request) {

    if (request.getMethod() == HttpMethod.OPTIONS) {
      return true;
    }

    String path = request.getURI().getPath();
    String downstreamPath = removeApiPrefix(path);

    if (request.getMethod() == HttpMethod.GET) {
      return PUBLIC_GET_ENDPOINTS.contains(downstreamPath);
    }

    if (request.getMethod() == HttpMethod.POST) {
      return PUBLIC_POST_ENDPOINTS.contains(downstreamPath);
    }

    return false;
  }

  private String removeApiPrefix(String path) {

    if (path.startsWith(apiPrefix)) {
      return path.substring(apiPrefix.length());
    }

    return path;
  }

  private boolean shouldBypassIntrospection(ServerHttpRequest request) {
    if (request.getMethod() != HttpMethod.POST) {
      return false;
    }

    String downstreamPath = removeApiPrefix(request.getURI().getPath());
    return NO_INTROSPECTION_POST_ENDPOINTS.contains(downstreamPath);
  }

  private String extractBearerToken(ServerHttpRequest request) {

    String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

    if (authorization == null || !authorization.startsWith("Bearer ")) {
      return null;
    }

    String token = authorization.substring(7).trim();

    return token.isEmpty() ? null : token;
  }

  private Mono<Void> writeError(ServerHttpResponse response, GatewayErrorCode errorCode) {

    response.setStatusCode(errorCode.getHttpStatus());
    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

    String body =
        """
                {"code":%d,"message":"%s","result":null}
                """
            .formatted(errorCode.getCode(), errorCode.getMessage())
            .trim();

    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);

    return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
  }

  @Override
  public int getOrder() {
    return -1;
  }
}
