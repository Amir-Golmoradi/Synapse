package dev.amir.synapse.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

  // Exposed as a bean so GoogleOAuthAdapter can inject RestClient.Builder
  // and apply its own base URL / headers without polluting a shared instance
  @Bean
  public RestClient.Builder restClientBuilder() {
    return RestClient.builder();
  }
}
