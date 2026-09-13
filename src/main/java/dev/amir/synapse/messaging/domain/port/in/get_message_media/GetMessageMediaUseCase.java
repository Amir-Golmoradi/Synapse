package dev.amir.synapse.messaging.domain.port.in.get_message_media;

import java.util.UUID;

@FunctionalInterface
public interface GetMessageMediaUseCase {
  AuthorizedMessageMedia handle(UUID messageId, UUID requesterId);
}
