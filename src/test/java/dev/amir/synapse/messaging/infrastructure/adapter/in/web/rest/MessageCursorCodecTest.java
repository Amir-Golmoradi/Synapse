package dev.amir.synapse.messaging.infrastructure.adapter.in.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.amir.synapse.messaging.domain.port.in.list_messages.MessageCursor;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class MessageCursorCodecTest {
  private final MessageCursorCodec codec = new MessageCursorCodec(JsonMapper.builder().build());

  @Test
  void roundTripsVersionedRoomScopedCursorWithoutPadding() {
    var roomId = UUID.randomUUID();
    var cursor = new MessageCursor(Instant.parse("2026-08-13T10:15:30.123456Z"), UUID.randomUUID());

    var encoded = codec.encode(roomId, cursor);

    assertThat(encoded).doesNotContain("=");
    assertThat(codec.decode(roomId, encoded)).isEqualTo(cursor);
    assertThat(new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8))
        .contains("\"v\":1")
        .contains("\"r\":\"" + roomId + "\"")
        .contains("\"t\":\"" + cursor.createdAt() + "\"")
        .contains("\"m\":\"" + cursor.messageId() + "\"");
  }

  @Test
  void rejectsMalformedNonCanonicalAndCrossRoomCursorsIdentically() {
    var roomId = UUID.randomUUID();
    var messageId = UUID.randomUUID();
    var validJson =
        "{\"v\":1,\"r\":\"%s\",\"t\":\"2026-08-13T10:00:00Z\",\"m\":\"%s\"}"
            .formatted(roomId, messageId);
    var invalidCursors =
        new String[] {
          "not+base64",
          encoded("not-json"),
          encoded(validJson.replace("\"v\":1", "\"v\":2")),
          encoded(validJson.replace(roomId.toString(), UUID.randomUUID().toString())),
          encoded(validJson.replace("2026-08-13T10:00:00Z", "not-an-instant")),
          encoded(validJson.replace("2026-08-13T10:00:00Z", Instant.MAX.toString())),
          encoded(validJson.replace("2026-08-13T10:00:00Z", Instant.MIN.toString())),
          encoded(validJson.replace("2026-08-13T10:00:00Z", "+100000-01-01T00:00:00Z")),
          encoded(validJson.replace("2026-08-13T10:00:00Z", "-100000-01-01T00:00:00Z")),
          encoded(validJson.replace(messageId.toString(), "not-a-uuid")),
          encoded(validJson) + "="
        };

    for (var invalidCursor : invalidCursors) {
      assertThatThrownBy(() -> codec.decode(roomId, invalidCursor))
          .isExactlyInstanceOf(InvalidMessageCursorException.class)
          .hasMessage("The message history cursor is invalid");
    }
  }

  private static String encoded(String value) {
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(value.getBytes(StandardCharsets.UTF_8));
  }
}
