package dev.amir.synapse.messaging.infrastructure.adapter.in.ws.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(WebSocketProperties.class)
class MessagingCorsConfig {

  @Bean
  CorsConfigurationSource corsConfigurationSource(WebSocketProperties properties) {
    var source = new UrlBasedCorsConfigurationSource();
    if (properties.allowedOrigins().isEmpty()) {
      return source;
    }

    var configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(properties.allowedOrigins());
    configuration.setAllowedMethods(List.of("GET"));
    configuration.setAllowedHeaders(List.of("Authorization"));
    configuration.setMaxAge(Duration.ofHours(1));
    source.registerCorsConfiguration("/api/v1/room/*/messages", configuration);
    return source;
  }
}
