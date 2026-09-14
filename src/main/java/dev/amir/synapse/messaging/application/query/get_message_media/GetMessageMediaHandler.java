package dev.amir.synapse.messaging.application.query.get_message_media;

import dev.amir.synapse.messaging.domain.exception.VideoMessageException;
import dev.amir.synapse.messaging.domain.port.in.get_message_media.AuthorizedMessageMedia;
import dev.amir.synapse.messaging.domain.port.in.get_message_media.GetMessageMediaUseCase;
import dev.amir.synapse.messaging.domain.port.out.MessageMediaReadPort;
import dev.amir.synapse.messaging.domain.port.out.MessageMediaStoragePort;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class GetMessageMediaHandler implements GetMessageMediaUseCase {
  private final MessageMediaReadPort media;
  private final MessageMediaStoragePort storage;

  public GetMessageMediaHandler(MessageMediaReadPort media, MessageMediaStoragePort storage) {
    this.media = media;
    this.storage = storage;
  }

  @Override
  public AuthorizedMessageMedia handle(UUID messageId, UUID requesterId) {
    var descriptor =
        media.findAuthorized(messageId, requesterId).orElseThrow(VideoMessageException::notFound);
    var path = storage.resolveForRead(descriptor.storageKey());
    return new AuthorizedMessageMedia(
        path, descriptor.contentType(), descriptor.sizeBytes(), descriptor.sha256());
  }
}
