package com.tbm.recruitment.notification.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfiguration {

  @Bean
  RestClient candidateRestClient(@Value("${services.candidate.base-url}") String candidateBaseUrl) {

    return RestClient.builder().baseUrl(candidateBaseUrl).build();
  }
}
