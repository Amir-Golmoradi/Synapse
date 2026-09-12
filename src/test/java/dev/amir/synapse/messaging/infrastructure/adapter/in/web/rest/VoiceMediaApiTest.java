package dev.amir.synapse.messaging.infrastructure.adapter.in.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.amir.synapse.messaging.domain.port.in.get_voice_media.GetVoiceMediaUseCase;
import dev.amir.synapse.messaging.domain.port.in.get_voice_media.VoiceMediaDownload;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

class VoiceMediaApiTest {
  private final GetVoiceMediaUseCase useCase = mock(GetVoiceMediaUseCase.class);
  private final VoiceMediaApi api = new VoiceMediaApi(useCase, new HttpByteRangeParser());

  @Test
  void returnsBoundedPartialContentWithSafeHeaders() throws Exception {
    var content = new byte[] {2, 3, 4};
    when(useCase.handle(any()))
        .thenReturn(
            new VoiceMediaDownload(
                new ByteArrayInputStream(content), "audio/ogg", 10, 2, content.length, true));
    var messageId = UUID.randomUUID();

    var response =
        api.download(authenticated(UUID.randomUUID()), UUID.randomUUID(), messageId, "bytes=2-4");
    var output = new ByteArrayOutputStream();
    response.getBody().writeTo(output);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PARTIAL_CONTENT);
    assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_RANGE)).isEqualTo("bytes 2-4/10");
    assertThat(response.getHeaders().getFirst(HttpHeaders.ACCEPT_RANGES)).isEqualTo("bytes");
    assertThat(response.getHeaders().getContentType().toString()).isEqualTo("audio/ogg");
    assertThat(response.getHeaders().getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
    assertThat(output.toByteArray()).containsExactly(content);
  }

  private static UsernamePasswordAuthenticationToken authenticated(UUID userId) {
    return new UsernamePasswordAuthenticationToken(userId.toString(), null, List.of());
  }
}
