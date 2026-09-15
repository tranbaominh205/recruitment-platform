package com.tbm.recruitment.gateway.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;

import com.tbm.recruitment.gateway.client.IdentityClient;
import com.tbm.recruitment.gateway.dto.response.IntrospectResult;
import com.tbm.recruitment.gateway.dto.response.IntrospectionApiResponse;
import java.util.List;
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

  @Test
  void authenticatedRequestPropagatesTrustedIdentityAndPermissionsHeaders() {
    MockServerHttpRequest request =
        MockServerHttpRequest.method(HttpMethod.GET, "/api/v1/identity/me")
            .header("Authorization", "Bearer test-token")
            .build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);
    AtomicBoolean chainCalled = new AtomicBoolean(false);
    org.mockito.Mockito.when(identityClient.introspect("test-token"))
        .thenReturn(
            Mono.just(
                new IntrospectionApiResponse(
                    1000,
                    "Success",
                    new IntrospectResult(
                        true,
                        "a7d1d5a9-6ee6-4ee7-97ff-78b6b92bb6e8",
                        "candidate@example.com",
                        "ADMIN",
                        List.of("ACCOUNT_DISABLE", "ACCOUNT_REVOKE_SESSIONS")))));
    GatewayFilterChain chain =
        gatewayExchange -> {
          chainCalled.set(true);
          assertEquals(
              "a7d1d5a9-6ee6-4ee7-97ff-78b6b92bb6e8",
              gatewayExchange.getRequest().getHeaders().getFirst("X-Account-Id"));
          assertEquals(
              "candidate@example.com",
              gatewayExchange.getRequest().getHeaders().getFirst("X-Account-Email"));
          assertEquals(
              "ADMIN", gatewayExchange.getRequest().getHeaders().getFirst("X-Account-Role"));
          assertEquals(
              "ACCOUNT_DISABLE,ACCOUNT_REVOKE_SESSIONS",
              gatewayExchange.getRequest().getHeaders().getFirst("X-Account-Permissions"));
          return Mono.empty();
        };

    authenticationFilter.filter(exchange, chain).block();

    assertTrue(chainCalled.get());
  }

  @Test
  void publicAndBypassRoutesRemoveSpoofedIdentityHeaders() {
    MockServerHttpRequest publicRequest =
        MockServerHttpRequest.method(HttpMethod.POST, "/api/v1/identity/auth/register")
            .header("X-Account-Id", "spoofed-user")
            .header("X-Account-Email", "spoof@example.com")
            .header("X-Account-Role", "ADMIN")
            .header("X-Account-Permissions", "ACCOUNT_DISABLE")
            .build();
    MockServerWebExchange publicExchange = MockServerWebExchange.from(publicRequest);
    AtomicBoolean publicChainCalled = new AtomicBoolean(false);
    GatewayFilterChain publicChain =
        gatewayExchange -> {
          publicChainCalled.set(true);
          assertEquals(null, gatewayExchange.getRequest().getHeaders().getFirst("X-Account-Id"));
          assertEquals(null, gatewayExchange.getRequest().getHeaders().getFirst("X-Account-Email"));
          assertEquals(null, gatewayExchange.getRequest().getHeaders().getFirst("X-Account-Role"));
          assertEquals(
              null, gatewayExchange.getRequest().getHeaders().getFirst("X-Account-Permissions"));
          return Mono.empty();
        };

    authenticationFilter.filter(publicExchange, publicChain).block();

    assertTrue(publicChainCalled.get());

    MockServerHttpRequest logoutRequest =
        MockServerHttpRequest.method(HttpMethod.POST, "/api/v1/identity/auth/logout")
            .header("Authorization", "Bearer test-token")
            .header("X-Account-Id", "spoofed-user")
            .header("X-Account-Email", "spoof@example.com")
            .header("X-Account-Role", "ADMIN")
            .header("X-Account-Permissions", "ACCOUNT_DISABLE")
            .build();
    MockServerWebExchange logoutExchange = MockServerWebExchange.from(logoutRequest);
    AtomicBoolean logoutChainCalled = new AtomicBoolean(false);
    GatewayFilterChain logoutChain =
        gatewayExchange -> {
          logoutChainCalled.set(true);
          assertEquals(null, gatewayExchange.getRequest().getHeaders().getFirst("X-Account-Id"));
          assertEquals(null, gatewayExchange.getRequest().getHeaders().getFirst("X-Account-Email"));
          assertEquals(null, gatewayExchange.getRequest().getHeaders().getFirst("X-Account-Role"));
          assertEquals(
              null, gatewayExchange.getRequest().getHeaders().getFirst("X-Account-Permissions"));
          return Mono.empty();
        };

    authenticationFilter.filter(logoutExchange, logoutChain).block();

    assertTrue(logoutChainCalled.get());
  }
}
