package dev.amir.synapse.messaging.domain.port.in.message;

import dev.amir.synapse.messaging.domain.enums.MessageType;
import dev.amir.synapse.messaging.domain.value_object.VoiceMessageMetadata;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record MessageView(
    UUID messageId,
    UUID roomId,
    UUID senderId,
    UUID clientMessageId,
    MessageType type,
    @Nullable String text,
    @Nullable VoiceMessageMetadata voice,
    Instant createdAt) {

  public MessageView {
    Objects.requireNonNull(messageId, "Message ID cannot be null");
    Objects.requireNonNull(roomId, "Room ID cannot be null");
    Objects.requireNonNull(senderId, "Sender ID cannot be null");
    Objects.requireNonNull(clientMessageId, "Client message ID cannot be null");
    Objects.requireNonNull(type, "Message type cannot be null");
    Objects.requireNonNull(createdAt, "Message creation timestamp cannot be null");
    if (type == MessageType.TEXT && (text == null || voice != null)) {
      throw new IllegalArgumentException(
          "Text messages require text and cannot contain voice metadata.");
    }
    if (type == MessageType.VOICE && (text != null || voice == null)) {
      throw new IllegalArgumentException(
          "Voice messages require voice metadata and cannot contain text.");
    }
  }

  public MessageView(
      UUID messageId,
      UUID roomId,
      UUID senderId,
      UUID clientMessageId,
      String text,
      Instant createdAt) {
    this(messageId, roomId, senderId, clientMessageId, MessageType.TEXT, text, null, createdAt);
  }
}
