package com.tbm.recruitment.job.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfiguration {

  @Bean
  RestClient.Builder restClientBuilder() {
    return RestClient.builder();
  }

  @Bean
  RestClient employerRestClient(
      RestClient.Builder builder, @Value("${services.employer.base-url}") String employerBaseUrl) {

    return builder.baseUrl(employerBaseUrl).build();
  }
}
