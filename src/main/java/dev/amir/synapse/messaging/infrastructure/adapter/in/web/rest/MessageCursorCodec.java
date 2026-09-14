package dev.amir.synapse.messaging.infrastructure.adapter.in.web.rest;

import dev.amir.synapse.messaging.domain.port.in.list_messages.MessageCursor;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class MessageCursorCodec {
  private static final int CURSOR_VERSION = 1;
  private static final int MAX_ENCODED_LENGTH = 1024;
  private static final Instant MIN_SUPPORTED_INSTANT = Instant.parse("0001-01-01T00:00:00Z");
  private static final Instant MAX_SUPPORTED_INSTANT = Instant.parse("9999-12-31T23:59:59.999999Z");
  private static final Pattern BASE64_URL_WITHOUT_PADDING = Pattern.compile("^[A-Za-z0-9_-]+$");

  private final ObjectMapper objectMapper;

  public MessageCursorCodec(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public String encode(UUID roomId, MessageCursor cursor) {
    Objects.requireNonNull(roomId, "Room ID cannot be null");
    Objects.requireNonNull(cursor, "Message cursor cannot be null");
    var payload =
        new CursorPayload(
            CURSOR_VERSION,
            roomId.toString(),
            cursor.createdAt().toString(),
            cursor.messageId().toString());

    try {
      return Base64.getUrlEncoder()
          .withoutPadding()
          .encodeToString(objectMapper.writeValueAsBytes(payload));
    } catch (RuntimeException exception) {
      throw new IllegalStateException("Message cursor could not be encoded", exception);
    }
  }

  public MessageCursor decode(UUID expectedRoomId, String encodedCursor) {
    Objects.requireNonNull(expectedRoomId, "Expected room ID cannot be null");

    try {
      validateEncodedForm(encodedCursor);
      var decoded = Base64.getUrlDecoder().decode(encodedCursor);
      var canonical = Base64.getUrlEncoder().withoutPadding().encodeToString(decoded);
      if (!canonical.equals(encodedCursor)) {
        throw invalidCursor();
      }

      var payload = objectMapper.readValue(decoded, CursorPayload.class);
      if (payload == null || payload.v() != CURSOR_VERSION) {
        throw invalidCursor();
      }

      var cursorRoomId = UUID.fromString(payload.r());
      if (!expectedRoomId.equals(cursorRoomId)) {
        throw invalidCursor();
      }

      var createdAt = Instant.parse(payload.t());
      if (createdAt.isBefore(MIN_SUPPORTED_INSTANT) || createdAt.isAfter(MAX_SUPPORTED_INSTANT)) {
        throw invalidCursor();
      }
      return new MessageCursor(createdAt, UUID.fromString(payload.m()));
    } catch (InvalidMessageCursorException exception) {
      throw exception;
    } catch (RuntimeException exception) {
      throw invalidCursor(exception);
    }
  }

  private static void validateEncodedForm(String encodedCursor) {
    if (encodedCursor == null
        || encodedCursor.isEmpty()
        || encodedCursor.length() > MAX_ENCODED_LENGTH
        || !BASE64_URL_WITHOUT_PADDING.matcher(encodedCursor).matches()) {
      throw invalidCursor();
    }
  }

  private static InvalidMessageCursorException invalidCursor() {
    return new InvalidMessageCursorException();
  }

  private static InvalidMessageCursorException invalidCursor(Throwable cause) {
    return new InvalidMessageCursorException(cause);
  }

  private record CursorPayload(int v, String r, String t, String m) {}
}
