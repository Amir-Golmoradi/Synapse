package dev.amir.synapse.messaging.application.query.list_messages;

import dev.amir.synapse.messaging.domain.port.in.list_messages.ListMessagesQuery;
import dev.amir.synapse.messaging.domain.port.in.list_messages.ListMessagesResult;
import dev.amir.synapse.messaging.domain.port.in.list_messages.ListMessagesUseCase;
import dev.amir.synapse.messaging.domain.port.in.list_messages.MessageCursor;
import dev.amir.synapse.messaging.domain.port.out.MessageHistoryPort;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListMessagesHandler implements ListMessagesUseCase {
  private final MessageHistoryPort messageHistoryPort;

  public ListMessagesHandler(MessageHistoryPort messageHistoryPort) {
    this.messageHistoryPort = messageHistoryPort;
  }

  @Override
  @Transactional(readOnly = true)
  public ListMessagesResult handle(ListMessagesQuery query) {
    var cursor = query.cursor();
    var messages =
        messageHistoryPort.findAuthorized(
            query.roomId(),
            query.requesterId(),
            query.limit() + 1,
            cursorCreatedAt(cursor),
            cursorMessageId(cursor));
    var hasAnotherPage = messages.size() > query.limit();
    var page = hasAnotherPage ? messages.subList(0, query.limit()) : messages;
    MessageCursor nextCursor = null;
    if (hasAnotherPage) {
      var lastMessage = page.getLast();
      nextCursor = new MessageCursor(lastMessage.createdAt(), lastMessage.messageId());
    }
    return new ListMessagesResult(page, nextCursor);
  }

  private static @Nullable Instant cursorCreatedAt(@Nullable MessageCursor cursor) {
    return cursor == null ? null : cursor.createdAt();
  }

  private static @Nullable UUID cursorMessageId(@Nullable MessageCursor cursor) {
    return cursor == null ? null : cursor.messageId();
  }
}
