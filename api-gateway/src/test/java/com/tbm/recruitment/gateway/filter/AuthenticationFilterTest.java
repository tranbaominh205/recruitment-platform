package com.tbm.recruitment.gateway.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;

import com.tbm.recruitment.gateway.client.IdentityClient;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

class AuthenticationFilterTest {

  private IdentityClient identityClient;
  private AuthenticationFilter authenticationFilter;

  @BeforeEach
  void setUp() {
    identityClient = org.mockito.Mockito.mock(IdentityClient.class);
    authenticationFilter = new AuthenticationFilter(identityClient);
    ReflectionTestUtils.setField(authenticationFilter, "apiPrefix", "/api/v1");
  }

  @Test
  void refreshEndpointBypassesAuthentication() {
    MockServerHttpRequest request =
        MockServerHttpRequest.method(HttpMethod.POST, "/api/v1/identity/auth/refresh").build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);
    AtomicBoolean chainCalled = new AtomicBoolean(false);
    GatewayFilterChain chain =
        gatewayExchange -> {
          chainCalled.set(true);
          return Mono.empty();
        };

    authenticationFilter.filter(exchange, chain).block();

    assertTrue(chainCalled.get());
    verifyNoInteractions(identityClient);
  }

  @Test
  void logoutEndpointRemainsProtectedWithoutAuthorizationHeader() {
    MockServerHttpRequest request =
        MockServerHttpRequest.method(HttpMethod.POST, "/api/v1/identity/auth/logout").build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);
    AtomicBoolean chainCalled = new AtomicBoolean(false);
    GatewayFilterChain chain =
        gatewayExchange -> {
          chainCalled.set(true);
          return Mono.empty();
        };

    authenticationFilter.filter(exchange, chain).block();

    assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    assertFalse(chainCalled.get());
    verifyNoInteractions(identityClient);
  }

  @Test
  void logoutEndpointBypassesIntrospectionWhenAuthorizationHeaderPresent() {
    MockServerHttpRequest request =
        MockServerHttpRequest.method(HttpMethod.POST, "/api/v1/identity/auth/logout")
            .header("Authorization", "Bearer expired-but-refreshable-token")
            .build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);
    AtomicBoolean chainCalled = new AtomicBoolean(false);
    GatewayFilterChain chain =
        gatewayExchange -> {
          chainCalled.set(true);
          return Mono.empty();
        };

    authenticationFilter.filter(exchange, chain).block();

    assertTrue(chainCalled.get());
    verifyNoInteractions(identityClient);
  }

  @Test
  void protectedIdentityEndpointRemainsProtectedWithoutAuthorizationHeader() {
    MockServerHttpRequest request =
        MockServerHttpRequest.method(HttpMethod.GET, "/api/v1/identity/me").build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);
    AtomicBoolean chainCalled = new AtomicBoolean(false);
    GatewayFilterChain chain =
        gatewayExchange -> {
          chainCalled.set(true);
          return Mono.empty();
        };

    authenticationFilter.filter(exchange, chain).block();

    assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    assertFalse(chainCalled.get());
    verifyNoInteractions(identityClient);
  }
}
