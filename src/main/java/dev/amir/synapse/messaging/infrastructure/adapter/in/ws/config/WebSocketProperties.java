package dev.amir.synapse.messaging.infrastructure.adapter.in.ws.config;

import java.util.List;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("synapse.websocket")
public record WebSocketProperties(List<String> allowedOrigins) {
  public WebSocketProperties {
    allowedOrigins =
        allowedOrigins == null
            ? List.of()
            : allowedOrigins.stream()
                .filter(Objects::nonNull)
                .map(String::strip)
                .filter(origin -> !origin.isEmpty())
                .distinct()
                .toList();

    if (allowedOrigins.contains("*")) {
      throw new IllegalArgumentException("Wildcard WebSocket origins are not allowed");
    }
  }
}
