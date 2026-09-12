package dev.amir.synapse.messaging.application.service;

import dev.amir.synapse.messaging.application.model.StoredVoiceMedia;
import dev.amir.synapse.messaging.application.port.out.MessagePublicationPort;
import dev.amir.synapse.messaging.domain.port.in.send_voice_message.SendVoiceMessageCommand;
import dev.amir.synapse.messaging.domain.port.out.VoiceMessageWritePort;
import dev.amir.synapse.messaging.domain.port.out.VoiceMessageWriteRequest;
import dev.amir.synapse.messaging.domain.port.out.VoiceMessageWriteResult;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VoiceMessageTransactionService {
  private final VoiceMessageWritePort writePort;
  private final MessagePublicationPort publicationPort;

  public VoiceMessageTransactionService(
      VoiceMessageWritePort writePort, MessagePublicationPort publicationPort) {
    this.writePort = writePort;
    this.publicationPort = publicationPort;
  }

  @Transactional
  public VoiceMessageWriteResult create(
      UUID messageId, SendVoiceMessageCommand command, StoredVoiceMedia media) {
    var result =
        writePort.saveAuthorized(
            new VoiceMessageWriteRequest(
                messageId,
                command.roomId(),
                command.senderId(),
                command.clientMessageId(),
                media.storageKey(),
                media.mimeType(),
                media.sizeBytes(),
                command.durationMs(),
                media.sha256()));
    publicationPort.publish(result.message());
    return result;
  }
}
