package dev.amir.synapse.messaging.infrastructure.adapter.in.ws.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class MessagingCorsConfigTest {

  @Test
  void allowsOnlyHistoryReadsFromExplicitMessagingOrigins() {
    var source =
        new MessagingCorsConfig()
            .corsConfigurationSource(
                new WebSocketProperties(List.of("https://app.example", "https://admin.example")));
    var historyRequest =
        new MockHttpServletRequest(
            "GET", "/api/v1/room/11111111-1111-1111-1111-111111111111/messages");

    var configuration = source.getCorsConfiguration(historyRequest);

    assertThat(configuration).isNotNull();
    assertThat(configuration.getAllowedOrigins())
        .containsExactly("https://app.example", "https://admin.example");
    assertThat(configuration.getAllowedMethods()).containsExactly("GET");
    assertThat(configuration.getAllowedHeaders()).containsExactly("Authorization");
    assertThat(configuration.getMaxAge()).isEqualTo(3600L);
    assertThat(
            source.getCorsConfiguration(
                new MockHttpServletRequest("GET", "/api/v1/room/111/messages/other")))
        .isNull();
  }

  @Test
  void emptyAllowlistLeavesHistorySameOriginOnly() {
    var source =
        new MessagingCorsConfig().corsConfigurationSource(new WebSocketProperties(List.of()));

    assertThat(
            source.getCorsConfiguration(
                new MockHttpServletRequest(
                    "GET", "/api/v1/room/11111111-1111-1111-1111-111111111111/messages")))
        .isNull();
  }
}
