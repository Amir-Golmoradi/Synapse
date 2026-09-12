package dev.amir.synapse.messaging.domain.value_object;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.amir.synapse.messaging.domain.exception.MessageValidationException;
import org.junit.jupiter.api.Test;

class VoiceMessageMetadataTest {
  @Test
  void normalizesSupportedMimeTypeAndAcceptsBoundaries() {
    assertThat(new VoiceMessageMetadata(1, " Audio/WebM; codecs=opus ", 1).mimeType())
        .isEqualTo("audio/webm");
    assertThat(
            new VoiceMessageMetadata(
                VoiceMessageMetadata.MAX_DURATION_MS,
                "audio/mp4",
                VoiceMessageMetadata.MAX_SIZE_BYTES))
        .isNotNull();
  }

  @Test
  void rejectsInvalidDurationSizeAndMimeType() {
    assertThatThrownBy(() -> new VoiceMessageMetadata(0, "audio/webm", 1))
        .isInstanceOf(MessageValidationException.class);
    assertThatThrownBy(() -> new VoiceMessageMetadata(1, "audio/webm", 0))
        .isInstanceOf(MessageValidationException.class);
    assertThatThrownBy(() -> new VoiceMessageMetadata(1, "audio/wav", 1))
        .isInstanceOf(MessageValidationException.class);
  }
}
