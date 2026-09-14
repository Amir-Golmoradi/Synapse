package dev.amir.synapse.messaging.application.query.list_messages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.amir.synapse.messaging.domain.port.in.list_messages.ListMessagesQuery;
import dev.amir.synapse.messaging.domain.port.in.list_messages.MessageCursor;
import dev.amir.synapse.messaging.domain.port.in.message.MessageView;
import dev.amir.synapse.messaging.domain.port.out.MessageHistoryPort;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ListMessagesHandlerTest {

  @Test
  void fetchesOneExtraItemAndCreatesCursorFromLastIncludedMessage() {
    var port = mock(MessageHistoryPort.class);
    var handler = new ListMessagesHandler(port);
    var requesterId = UUID.randomUUID();
    var roomId = UUID.randomUUID();
    var sourceCursor = new MessageCursor(Instant.parse("2026-08-13T12:00:00Z"), UUID.randomUUID());
    var first = message(roomId, Instant.parse("2026-08-13T11:59:00Z"));
    var second = message(roomId, Instant.parse("2026-08-13T11:58:00Z"));
    var extra = message(roomId, Instant.parse("2026-08-13T11:57:00Z"));
    when(port.findAuthorized(
            roomId, requesterId, 3, sourceCursor.createdAt(), sourceCursor.messageId()))
        .thenReturn(List.of(first, second, extra));

    var result = handler.handle(new ListMessagesQuery(requesterId, roomId, 2, sourceCursor));

    assertThat(result.items()).containsExactly(first, second);
    assertThat(result.nextCursor())
        .isEqualTo(new MessageCursor(second.createdAt(), second.messageId()));
    verify(port)
        .findAuthorized(roomId, requesterId, 3, sourceCursor.createdAt(), sourceCursor.messageId());
  }

  @Test
  void omitsNextCursorWhenThePageIsExhausted() {
    var port = mock(MessageHistoryPort.class);
    var handler = new ListMessagesHandler(port);
    var requesterId = UUID.randomUUID();
    var roomId = UUID.randomUUID();
    var onlyMessage = message(roomId, Instant.parse("2026-08-13T11:59:00Z"));
    when(port.findAuthorized(roomId, requesterId, 51, null, null)).thenReturn(List.of(onlyMessage));

    var result = handler.handle(new ListMessagesQuery(requesterId, roomId, 50, null));

    assertThat(result.items()).containsExactly(onlyMessage);
    assertThat(result.nextCursor()).isNull();
  }

  private static MessageView message(UUID roomId, Instant createdAt) {
    return new MessageView(
        UUID.randomUUID(), roomId, UUID.randomUUID(), UUID.randomUUID(), "message", createdAt);
  }
}
