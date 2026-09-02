package com.tbm.recruitment.recruitment.configuration;

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

  @Bean
  RestClient resumeRestClient(@Value("${services.resume.base-url}") String resumeBaseUrl) {

    return RestClient.builder().baseUrl(resumeBaseUrl).build();
  }

  @Bean
  RestClient jobRestClient(@Value("${services.job.base-url}") String jobBaseUrl) {

    return RestClient.builder().baseUrl(jobBaseUrl).build();
  }
}
