package dev.amir.synapse.messaging.domain.port.out;

import dev.amir.synapse.messaging.domain.port.in.message.MessageView;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

@FunctionalInterface
public interface MessageHistoryPort {
  List<MessageView> findAuthorized(
      UUID roomId,
      UUID requesterId,
      int fetchSize,
      @Nullable Instant beforeCreatedAt,
      @Nullable UUID beforeMessageId);
}
