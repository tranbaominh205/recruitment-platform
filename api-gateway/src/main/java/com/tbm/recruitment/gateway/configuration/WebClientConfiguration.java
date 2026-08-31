package com.tbm.recruitment.gateway.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfiguration {

  @Bean
  WebClient identityWebClient(@Value("${services.identity.url}") String identityServiceUrl) {

    return WebClient.builder().baseUrl(identityServiceUrl).build();
  }
}
