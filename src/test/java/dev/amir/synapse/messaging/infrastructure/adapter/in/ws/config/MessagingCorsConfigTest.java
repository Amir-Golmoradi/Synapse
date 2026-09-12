package dev.amir.synapse.messaging.infrastructure.adapter.in.ws.config;

import static org.assertj.core.api.Assertions.assertThat;

import dev.amir.synapse.shared.config.ApiCorsConfig;
import dev.amir.synapse.shared.websocket.config.WebSocketProperties;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class MessagingCorsConfigTest {

  @Test
  void allowsOnlyHistoryReadsFromExplicitMessagingOrigins() {
    var source =
        new ApiCorsConfig()
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
    assertThat(configuration.getAllowedHeaders())
        .containsExactly("Authorization", "Content-Type", "Range", "X-Request-ID");
    assertThat(configuration.getMaxAge()).isEqualTo(3600L);
    assertThat(
            source.getCorsConfiguration(
                new MockHttpServletRequest("GET", "/api/v1/room/111/messages/other")))
        .isNull();
  }

  @Test
  void configuresVoiceUploadAndPlaybackWithoutBroadeningOtherRoutes() {
    var source =
        new ApiCorsConfig()
            .corsConfigurationSource(new WebSocketProperties(List.of("https://app.example")));
    var upload =
        source.getCorsConfiguration(
            new MockHttpServletRequest(
                "POST", "/api/v1/room/11111111-1111-1111-1111-111111111111/messages/voice"));
    var playback =
        source.getCorsConfiguration(
            new MockHttpServletRequest(
                "GET",
                "/api/v1/room/11111111-1111-1111-1111-111111111111/messages/22222222-2222-2222-2222-222222222222/media"));

    assertThat(upload).isNotNull();
    assertThat(upload.getAllowedMethods()).containsExactly("POST");
    assertThat(playback).isNotNull();
    assertThat(playback.getAllowedMethods()).containsExactly("GET", "HEAD");
    assertThat(playback.getAllowedHeaders()).contains("Authorization", "Range");
    assertThat(playback.getExposedHeaders())
        .contains("Accept-Ranges", "Content-Range", "Content-Length", "Content-Disposition");
  }

  @Test
  void emptyAllowlistLeavesHistorySameOriginOnly() {
    var source = new ApiCorsConfig().corsConfigurationSource(new WebSocketProperties(List.of()));

    assertThat(
            source.getCorsConfiguration(
                new MockHttpServletRequest(
                    "GET", "/api/v1/room/11111111-1111-1111-1111-111111111111/messages")))
        .isNull();
  }
}
