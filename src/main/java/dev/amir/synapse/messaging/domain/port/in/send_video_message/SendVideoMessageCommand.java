package dev.amir.synapse.messaging.domain.port.in.send_video_message;

import dev.amir.synapse.messaging.domain.exception.MessageValidationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public record SendVideoMessageCommand(
    UUID senderId,
    UUID roomId,
    UUID clientMessageId,
    String declaredContentType,
    long declaredSize,
    VideoContentSource content) {

  public SendVideoMessageCommand {
    if (senderId == null || roomId == null || clientMessageId == null) {
      throw new MessageValidationException("Video message identifiers are required.");
    }
    if (declaredContentType == null || declaredContentType.isBlank()) {
      throw new MessageValidationException("Video content type is required.");
    }
    if (declaredSize <= 0 || content == null) {
      throw new MessageValidationException("A non-empty video is required.");
    }
  }

  @FunctionalInterface
  public interface VideoContentSource {
    InputStream open() throws IOException;
  }
}
