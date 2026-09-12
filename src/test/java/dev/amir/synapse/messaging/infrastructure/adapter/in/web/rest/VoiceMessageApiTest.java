package dev.amir.synapse.messaging.infrastructure.adapter.in.web.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.amir.synapse.identity.domain.port.in.access_token.AuthenticateAccessTokenUseCase;
import dev.amir.synapse.messaging.domain.enums.MessageType;
import dev.amir.synapse.messaging.domain.port.in.message.MessageView;
import dev.amir.synapse.messaging.domain.port.in.send_voice_message.SendVoiceMessageCommand;
import dev.amir.synapse.messaging.domain.port.in.send_voice_message.SendVoiceMessageResult;
import dev.amir.synapse.messaging.domain.port.in.send_voice_message.SendVoiceMessageUseCase;
import dev.amir.synapse.messaging.domain.value_object.VoiceMessageMetadata;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    value = VoiceMessageApi.class,
    excludeAutoConfiguration = OAuth2ClientAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
class VoiceMessageApiTest {
  @Autowired private MockMvc mockMvc;
  @MockitoBean private SendVoiceMessageUseCase useCase;
  @MockitoBean private AuthenticateAccessTokenUseCase authenticateAccessTokenUseCase;

  @Test
  void uploadsMultipartAudioAsAuthenticatedSender() throws Exception {
    var senderId = UUID.randomUUID();
    var roomId = UUID.randomUUID();
    var clientMessageId = UUID.randomUUID();
    var audioBytes = "OggS--OpusHead--audio".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    var message =
        new MessageView(
            UUID.randomUUID(),
            roomId,
            senderId,
            clientMessageId,
            MessageType.VOICE,
            null,
            new VoiceMessageMetadata(2_500, "audio/ogg", audioBytes.length),
            Instant.parse("2026-09-12T12:00:00Z"));
    when(useCase.handle(any())).thenReturn(new SendVoiceMessageResult(message, true));
    var metadata =
        new MockMultipartFile(
            "metadata",
            "metadata.json",
            MediaType.APPLICATION_JSON_VALUE,
            ("{\"clientMessageId\":\"" + clientMessageId + "\",\"durationMs\":2500}")
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
    var audio = new MockMultipartFile("audio", "recording.ogg", "audio/ogg", audioBytes);

    mockMvc
        .perform(
            multipart("/api/v1/room/{roomId}/messages/voice", roomId)
                .file(metadata)
                .file(audio)
                .principal(authenticated(senderId)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.type").value("VOICE"))
        .andExpect(jsonPath("$.text").isEmpty())
        .andExpect(jsonPath("$.voice.durationMs").value(2_500))
        .andExpect(jsonPath("$.voice.mimeType").value("audio/ogg"));

    var command = ArgumentCaptor.forClass(SendVoiceMessageCommand.class);
    verify(useCase).handle(command.capture());
    org.assertj.core.api.Assertions.assertThat(command.getValue().senderId()).isEqualTo(senderId);
    org.assertj.core.api.Assertions.assertThat(command.getValue().roomId()).isEqualTo(roomId);
    org.assertj.core.api.Assertions.assertThat(command.getValue().declaredSizeBytes())
        .isEqualTo(audioBytes.length);
  }

  private static UsernamePasswordAuthenticationToken authenticated(UUID userId) {
    return new UsernamePasswordAuthenticationToken(userId.toString(), null, List.of());
  }
}
