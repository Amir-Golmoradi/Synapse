package dev.amir.synapse.messaging.domain.port.in.send_voice_message;

import dev.amir.synapse.messaging.domain.exception.MessageValidationException;
import dev.amir.synapse.messaging.domain.exception.UnsupportedVoiceMediaException;
import dev.amir.synapse.messaging.domain.exception.VoiceMediaTooLargeException;
import dev.amir.synapse.messaging.domain.value_object.VoiceMessageMetadata;
import java.util.Objects;
import java.util.UUID;

public record SendVoiceMessageCommand(
    UUID senderId,
    UUID roomId,
    UUID clientMessageId,
    int durationMs,
    String declaredMimeType,
    long declaredSizeBytes,
    VoiceUploadSource uploadSource) {

  public SendVoiceMessageCommand {
    requireId(senderId, "Sender ID");
    requireId(roomId, "Room ID");
    requireId(clientMessageId, "Client message ID");
    if (declaredSizeBytes > VoiceMessageMetadata.MAX_SIZE_BYTES) {
      throw new VoiceMediaTooLargeException();
    }
    var normalizedMimeType = VoiceMessageMetadata.normalizeMimeType(declaredMimeType);
    if (!VoiceMessageMetadata.SUPPORTED_MIME_TYPES.contains(normalizedMimeType)) {
      throw new UnsupportedVoiceMediaException();
    }
    new VoiceMessageMetadata(durationMs, normalizedMimeType, declaredSizeBytes);
    Objects.requireNonNull(uploadSource, "Voice upload source cannot be null");
    declaredMimeType = normalizedMimeType;
  }

  private static void requireId(UUID id, String label) {
    if (id == null) {
      throw new MessageValidationException(label + " cannot be null.");
    }
  }
}
