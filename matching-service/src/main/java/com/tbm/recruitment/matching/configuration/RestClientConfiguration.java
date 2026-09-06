package com.tbm.recruitment.matching.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfiguration {

  @Bean
  RestClient resumeRestClient(@Value("${services.resume.base-url}") String resumeBaseUrl) {
    return RestClient.builder().baseUrl(resumeBaseUrl).build();
  }

  @Bean
  RestClient recruitmentRestClient(
      @Value("${services.recruitment.base-url}") String recruitmentBaseUrl) {
    return RestClient.builder().baseUrl(recruitmentBaseUrl).build();
  }

  @Bean
  RestClient jobRestClient(@Value("${services.job.base-url}") String jobBaseUrl) {
    return RestClient.builder().baseUrl(jobBaseUrl).build();
  }
}
