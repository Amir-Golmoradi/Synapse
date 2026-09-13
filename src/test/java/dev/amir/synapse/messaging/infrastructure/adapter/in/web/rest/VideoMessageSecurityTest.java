package dev.amir.synapse.messaging.infrastructure.adapter.in.web.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.amir.synapse.identity.domain.port.in.access_token.AuthenticateAccessTokenUseCase;
import dev.amir.synapse.messaging.domain.port.in.get_message_media.GetMessageMediaUseCase;
import dev.amir.synapse.messaging.domain.port.in.send_video_message.SendVideoMessageUseCase;
import dev.amir.synapse.shared.config.ApiCorsConfig;
import dev.amir.synapse.shared.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    value = {VideoMessageCommandApi.class, MessageMediaQueryApi.class},
    excludeAutoConfiguration = OAuth2ClientAutoConfiguration.class)
@Import({SecurityConfig.class, ApiCorsConfig.class})
@TestPropertySource(properties = "synapse.websocket.allowed-origins=https://app.example")
class VideoMessageSecurityTest {
  private static final String ROOM_ID = "11111111-1111-1111-1111-111111111111";
  private static final String MESSAGE_ID = "22222222-2222-2222-2222-222222222222";

  @Autowired private MockMvc mockMvc;

  @MockitoBean private SendVideoMessageUseCase sendVideoMessage;
  @MockitoBean private GetMessageMediaUseCase getMessageMedia;
  @MockitoBean private AuthenticateAccessTokenUseCase authenticateAccessTokenUseCase;

  @Test
  void uploadAndPlaybackRequireAuthentication() throws Exception {
    mockMvc
        .perform(multipart("/api/v1/room/{roomId}/messages/video", ROOM_ID))
        .andExpect(status().isUnauthorized());
    mockMvc
        .perform(get("/api/v1/messages/{messageId}/media", MESSAGE_ID))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void allowsExplicitOriginPreflightsIncludingRangeHeader() throws Exception {
    mockMvc
        .perform(
            options("/api/v1/room/{roomId}/messages/video", ROOM_ID)
                .header(HttpHeaders.ORIGIN, "https://app.example")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, HttpHeaders.AUTHORIZATION))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://app.example"))
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "POST"));

    mockMvc
        .perform(
            options("/api/v1/messages/{messageId}/media", MESSAGE_ID)
                .header(HttpHeaders.ORIGIN, "https://app.example")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, HttpHeaders.RANGE))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://app.example"))
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET,HEAD"))
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, HttpHeaders.RANGE));
  }

  @Test
  void rejectsUntrustedPlaybackOrigins() throws Exception {
    mockMvc
        .perform(
            options("/api/v1/messages/{messageId}/media", MESSAGE_ID)
                .header(HttpHeaders.ORIGIN, "https://untrusted.example")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
        .andExpect(status().isForbidden());
  }
}
