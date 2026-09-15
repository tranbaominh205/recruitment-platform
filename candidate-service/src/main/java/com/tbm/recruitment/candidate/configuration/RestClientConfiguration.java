package com.tbm.recruitment.candidate.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfiguration {

  @Bean
  RestClient jobRestClient(@Value("${services.job.base-url}") String jobBaseUrl) {
    return RestClient.builder().baseUrl(jobBaseUrl).build();
  }
}
