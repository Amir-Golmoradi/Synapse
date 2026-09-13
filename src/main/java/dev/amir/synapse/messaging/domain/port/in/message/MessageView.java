package dev.amir.synapse.messaging.domain.port.in.message;

import dev.amir.synapse.messaging.domain.enums.MessageType;
import dev.amir.synapse.messaging.domain.value_object.VideoMetadata;
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
    @Nullable VideoMetadata media,
    Instant createdAt) {

  public MessageView(
      UUID messageId,
      UUID roomId,
      UUID senderId,
      UUID clientMessageId,
      String text,
      Instant createdAt) {
    this(messageId, roomId, senderId, clientMessageId, MessageType.TEXT, text, null, createdAt);
  }

  public MessageView {
    Objects.requireNonNull(messageId, "Message ID cannot be null");
    Objects.requireNonNull(roomId, "Room ID cannot be null");
    Objects.requireNonNull(senderId, "Sender ID cannot be null");
    Objects.requireNonNull(clientMessageId, "Client message ID cannot be null");
    Objects.requireNonNull(type, "Message type cannot be null");
    Objects.requireNonNull(createdAt, "Message creation timestamp cannot be null");
    if (type == MessageType.TEXT && (text == null || media != null)) {
      throw new IllegalArgumentException("A text message requires text and cannot contain media");
    }
    if (type == MessageType.VIDEO && (text != null || media == null)) {
      throw new IllegalArgumentException("A video message requires media and cannot contain text");
    }
  }
}
