package dev.amir.synapse.messaging.infrastructure.adapter.in.web.rest;

import dev.amir.synapse.messaging.domain.port.in.message.MessageView;
import java.util.List;
import org.jspecify.annotations.Nullable;

public record MessageHistoryResponse(List<MessageView> items, @Nullable String nextCursor) {
  public MessageHistoryResponse {
    items = List.copyOf(items);
  }
}
