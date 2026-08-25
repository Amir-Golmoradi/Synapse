package dev.amir.synapse.messaging.infrastructure.adapter.in.ws.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.amir.synapse.identity.domain.port.in.access_token.AuthenticateAccessTokenUseCase;
import dev.amir.synapse.messaging.domain.port.in.list_messages.ListMessagesUseCase;
import dev.amir.synapse.messaging.infrastructure.adapter.in.web.rest.MessageCursorCodec;
import dev.amir.synapse.messaging.infrastructure.adapter.in.web.rest.MessageQueryApi;
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
    value = MessageQueryApi.class,
    excludeAutoConfiguration = OAuth2ClientAutoConfiguration.class)
@Import({SecurityConfig.class, MessagingCorsConfig.class})
@TestPropertySource(properties = "synapse.websocket.allowed-origins=https://app.example")
class MessageHistoryCorsSecurityTest {
  private static final String HISTORY_PATH =
      "/api/v1/room/11111111-1111-1111-1111-111111111111/messages";

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ListMessagesUseCase listMessagesUseCase;
  @MockitoBean private MessageCursorCodec cursorCodec;
  @MockitoBean private AuthenticateAccessTokenUseCase authenticateAccessTokenUseCase;

  @Test
  void securityUsesTheMessagingOwnedCorsPolicy() throws Exception {
    mockMvc
        .perform(
            options(HISTORY_PATH)
                .header(HttpHeaders.ORIGIN, "https://app.example")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, HttpHeaders.AUTHORIZATION))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://app.example"))
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET"))
        .andExpect(
            header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, HttpHeaders.AUTHORIZATION));
  }

  @Test
  void rejectsOriginsOutsideTheExplicitAllowlist() throws Exception {
    mockMvc
        .perform(
            options(HISTORY_PATH)
                .header(HttpHeaders.ORIGIN, "https://untrusted.example")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
        .andExpect(status().isForbidden());
  }
}
