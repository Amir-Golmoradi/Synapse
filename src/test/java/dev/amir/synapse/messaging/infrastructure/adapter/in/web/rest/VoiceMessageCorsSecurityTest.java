package dev.amir.synapse.messaging.infrastructure.adapter.in.web.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.amir.synapse.identity.domain.port.in.access_token.AuthenticateAccessTokenUseCase;
import dev.amir.synapse.messaging.domain.port.in.get_voice_media.GetVoiceMediaUseCase;
import dev.amir.synapse.messaging.domain.port.in.send_voice_message.SendVoiceMessageUseCase;
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
    value = {VoiceMessageApi.class, VoiceMediaApi.class},
    excludeAutoConfiguration = OAuth2ClientAutoConfiguration.class)
@Import({SecurityConfig.class, ApiCorsConfig.class, HttpByteRangeParser.class})
@TestPropertySource(properties = "synapse.websocket.allowed-origins=https://app.example")
class VoiceMessageCorsSecurityTest {
  private static final String ROOM_ID = "11111111-1111-1111-1111-111111111111";
  private static final String MESSAGE_ID = "22222222-2222-2222-2222-222222222222";
  private static final String UPLOAD_PATH = "/api/v1/room/" + ROOM_ID + "/messages/voice";
  private static final String PLAYBACK_PATH =
      "/api/v1/room/" + ROOM_ID + "/messages/" + MESSAGE_ID + "/media";

  @Autowired private MockMvc mockMvc;
  @MockitoBean private SendVoiceMessageUseCase sendVoiceMessageUseCase;
  @MockitoBean private GetVoiceMediaUseCase getVoiceMediaUseCase;
  @MockitoBean private AuthenticateAccessTokenUseCase authenticateAccessTokenUseCase;

  @Test
  void unauthenticatedUploadAndPlaybackAreRejected() throws Exception {
    mockMvc.perform(multipart(UPLOAD_PATH)).andExpect(status().isUnauthorized());
    mockMvc.perform(get(PLAYBACK_PATH)).andExpect(status().isUnauthorized());
  }

  @Test
  void voiceCorsPolicyAllowsUploadAndRangePlaybackFromTrustedOrigin() throws Exception {
    mockMvc
        .perform(
            options(UPLOAD_PATH)
                .header(HttpHeaders.ORIGIN, "https://app.example")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, HttpHeaders.AUTHORIZATION))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "POST"));

    mockMvc
        .perform(
            options(PLAYBACK_PATH)
                .header(HttpHeaders.ORIGIN, "https://app.example")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .header(
                    HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS,
                    HttpHeaders.AUTHORIZATION + "," + HttpHeaders.RANGE))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://app.example"))
        .andExpect(
            header()
                .string(
                    HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                    org.hamcrest.Matchers.containsString(HttpHeaders.RANGE)));
  }
}
