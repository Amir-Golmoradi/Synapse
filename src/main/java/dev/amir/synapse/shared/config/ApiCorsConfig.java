package dev.amir.synapse.shared.config;

import dev.amir.synapse.shared.websocket.config.WebSocketProperties;
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
public class ApiCorsConfig {
  @Bean
  public CorsConfigurationSource corsConfigurationSource(WebSocketProperties properties) {
    var source = new UrlBasedCorsConfigurationSource();
    if (properties.allowedOrigins().isEmpty()) {
      return source;
    }
    source.registerCorsConfiguration(
        "/api/v1/room/*/messages", configuration(properties, List.of("GET")));
    source.registerCorsConfiguration(
        "/api/v1/calls", configuration(properties, List.of("GET", "POST")));
    source.registerCorsConfiguration(
        "/api/v1/calls/**", configuration(properties, List.of("GET", "POST")));
    source.registerCorsConfiguration(
        "/api/v1/auth/refresh", configuration(properties, List.of("POST")));
    return source;
  }

  private static CorsConfiguration configuration(
      WebSocketProperties properties, List<String> methods) {
    var configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(properties.allowedOrigins());
    configuration.setAllowedMethods(methods);
    configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Request-ID"));
    configuration.setExposedHeaders(List.of("Location", "X-Request-ID"));
    configuration.setMaxAge(Duration.ofHours(1));
    return configuration;
  }
}
