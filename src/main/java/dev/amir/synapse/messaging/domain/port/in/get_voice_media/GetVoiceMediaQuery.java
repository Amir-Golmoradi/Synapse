package dev.amir.synapse.messaging.domain.port.in.get_voice_media;

import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record GetVoiceMediaQuery(
    UUID requesterId, UUID roomId, UUID messageId, @Nullable MediaRangeRequest range) {
  public GetVoiceMediaQuery {
    Objects.requireNonNull(requesterId, "Requester ID cannot be null");
    Objects.requireNonNull(roomId, "Room ID cannot be null");
    Objects.requireNonNull(messageId, "Message ID cannot be null");
  }
}
