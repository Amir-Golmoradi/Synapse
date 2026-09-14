package dev.amir.synapse.messaging.domain.port.in.list_messages;

import dev.amir.synapse.messaging.domain.port.in.message.MessageView;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

public record ListMessagesResult(List<MessageView> items, @Nullable MessageCursor nextCursor) {
  public ListMessagesResult {
    Objects.requireNonNull(items, "Messages cannot be null");
    items = List.copyOf(items);
  }
}
