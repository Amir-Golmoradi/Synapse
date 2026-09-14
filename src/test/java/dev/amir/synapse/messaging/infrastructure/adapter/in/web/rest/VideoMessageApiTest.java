package dev.amir.synapse.messaging.infrastructure.adapter.in.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.amir.synapse.identity.domain.port.in.access_token.AuthenticateAccessTokenUseCase;
import dev.amir.synapse.messaging.domain.enums.MessageType;
import dev.amir.synapse.messaging.domain.exception.VideoMessageException;
import dev.amir.synapse.messaging.domain.port.in.get_message_media.AuthorizedMessageMedia;
import dev.amir.synapse.messaging.domain.port.in.get_message_media.GetMessageMediaUseCase;
import dev.amir.synapse.messaging.domain.port.in.message.MessageView;
import dev.amir.synapse.messaging.domain.port.in.send_video_message.SendVideoMessageCommand;
import dev.amir.synapse.messaging.domain.port.in.send_video_message.SendVideoMessageResult;
import dev.amir.synapse.messaging.domain.port.in.send_video_message.SendVideoMessageUseCase;
import dev.amir.synapse.messaging.domain.value_object.VideoMetadata;
import dev.amir.synapse.messaging.domain.value_object.VideoMetadata.AudioCodec;
import dev.amir.synapse.messaging.domain.value_object.VideoMetadata.VideoCodec;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    value = {VideoMessageCommandApi.class, MessageMediaQueryApi.class},
    excludeAutoConfiguration = OAuth2ClientAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
class VideoMessageApiTest {
  @Autowired private MockMvc mockMvc;

  @MockitoBean private SendVideoMessageUseCase sendVideoMessage;
  @MockitoBean private GetMessageMediaUseCase getMessageMedia;
  @MockitoBean private AuthenticateAccessTokenUseCase authenticateAccessTokenUseCase;
  @TempDir Path temporaryDirectory;

  @Test
  void uploadsMultipartVideoAndReturnsCreatedMessage() throws Exception {
    var userId = UUID.randomUUID();
    var roomId = UUID.randomUUID();
    var clientMessageId = UUID.randomUUID();
    var metadata =
        new VideoMetadata("video/webm", 5, 1_500, 640, 480, VideoCodec.VP9, AudioCodec.OPUS);
    var message =
        new MessageView(
            UUID.randomUUID(),
            roomId,
            userId,
            clientMessageId,
            MessageType.VIDEO,
            null,
            metadata,
            Instant.parse("2026-09-13T00:00:00Z"));
    when(sendVideoMessage.handle(any())).thenReturn(new SendVideoMessageResult(message, true));
    var video = new MockMultipartFile("video", "capture.webm", "video/webm", new byte[5]);

    mockMvc
        .perform(
            multipart("/api/v1/room/{roomId}/messages/video", roomId)
                .file(video)
                .param("clientMessageId", clientMessageId.toString())
                .principal(authenticated(userId)))
        .andExpect(status().isCreated())
        .andExpect(
            header()
                .string(HttpHeaders.LOCATION, "/api/v1/messages/" + message.messageId() + "/media"))
        .andExpect(jsonPath("$.type").value("VIDEO"))
        .andExpect(jsonPath("$.text").doesNotExist())
        .andExpect(jsonPath("$.media.contentType").value("video/webm"))
        .andExpect(jsonPath("$.media.durationMs").value(1_500));

    var captor = ArgumentCaptor.forClass(SendVideoMessageCommand.class);
    verify(sendVideoMessage).handle(captor.capture());
    assertThat(captor.getValue())
        .satisfies(
            command -> {
              assertThat(command.senderId()).isEqualTo(userId);
              assertThat(command.roomId()).isEqualTo(roomId);
              assertThat(command.clientMessageId()).isEqualTo(clientMessageId);
              assertThat(command.declaredContentType()).isEqualTo("video/webm");
              assertThat(command.declaredSize()).isEqualTo(5);
            });
  }

  @Test
  void mapsUploadLimitFailuresToSanitizedProblemDetails() throws Exception {
    var userId = UUID.randomUUID();
    var roomId = UUID.randomUUID();
    var clientMessageId = UUID.randomUUID();
    when(sendVideoMessage.handle(any())).thenThrow(VideoMessageException.tooLarge());

    mockMvc
        .perform(
            multipart("/api/v1/room/{roomId}/messages/video", roomId)
                .file(new MockMultipartFile("video", "capture.webm", "video/webm", new byte[1]))
                .param("clientMessageId", clientMessageId.toString())
                .principal(authenticated(userId)))
        .andExpect(status().isPayloadTooLarge())
        .andExpect(jsonPath("$.errorCode").value("VIDEO_FILE_TOO_LARGE"))
        .andExpect(jsonPath("$.detail").value("The video exceeds the size limit."));
  }

  @Test
  void servesAuthorizedMediaWithPrivateCachingAndByteRanges() throws Exception {
    var userId = UUID.randomUUID();
    var messageId = UUID.randomUUID();
    var file = temporaryDirectory.resolve("message.webm");
    Files.write(file, "0123456789".getBytes());
    var digest = new byte[32];
    when(getMessageMedia.handle(messageId, userId))
        .thenReturn(new AuthorizedMessageMedia(file, "video/webm", 10, digest));

    mockMvc
        .perform(
            get("/api/v1/messages/{messageId}/media", messageId)
                .header(HttpHeaders.RANGE, "bytes=2-5")
                .principal(authenticated(userId)))
        .andExpect(status().isPartialContent())
        .andExpect(header().string(HttpHeaders.ACCEPT_RANGES, "bytes"))
        .andExpect(header().string(HttpHeaders.CONTENT_RANGE, "bytes 2-5/10"))
        .andExpect(
            header().string(HttpHeaders.CACHE_CONTROL, "max-age=3600, no-transform, private"))
        .andExpect(header().string("X-Content-Type-Options", "nosniff"))
        .andExpect(content().bytes("2345".getBytes()));
  }

  @Test
  void doesNotRevealWhetherMissingMediaExists() throws Exception {
    var userId = UUID.randomUUID();
    var messageId = UUID.randomUUID();
    when(getMessageMedia.handle(messageId, userId)).thenThrow(VideoMessageException.notFound());

    mockMvc
        .perform(
            get("/api/v1/messages/{messageId}/media", messageId).principal(authenticated(userId)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("MESSAGE_MEDIA_NOT_FOUND"))
        .andExpect(jsonPath("$.detail").value("The media was not found or is not accessible."));
  }

  private static UsernamePasswordAuthenticationToken authenticated(UUID userId) {
    return new UsernamePasswordAuthenticationToken(userId.toString(), null, List.of());
  }
}
