package dev.amir.synapse.messaging.application.command.send_voice_message;

import dev.amir.synapse.messaging.application.model.StoredVoiceMedia;
import dev.amir.synapse.messaging.application.port.out.VoiceMediaStoragePort;
import dev.amir.synapse.messaging.application.service.VoiceMessageTransactionService;
import dev.amir.synapse.messaging.domain.exception.MessageRoomAccessDeniedException;
import dev.amir.synapse.messaging.domain.exception.VoiceMediaStorageException;
import dev.amir.synapse.messaging.domain.port.in.send_voice_message.SendVoiceMessageCommand;
import dev.amir.synapse.messaging.domain.port.in.send_voice_message.SendVoiceMessageResult;
import dev.amir.synapse.messaging.domain.port.in.send_voice_message.SendVoiceMessageUseCase;
import dev.amir.synapse.messaging.domain.port.out.MessageSendAuthorizationPort;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SendVoiceMessageHandler implements SendVoiceMessageUseCase {
  private static final Logger LOGGER = LoggerFactory.getLogger(SendVoiceMessageHandler.class);

  private final MessageSendAuthorizationPort authorizationPort;
  private final VoiceMediaStoragePort storagePort;
  private final VoiceMessageTransactionService transactionService;
  private final MeterRegistry meterRegistry;

  public SendVoiceMessageHandler(
      MessageSendAuthorizationPort authorizationPort,
      VoiceMediaStoragePort storagePort,
      VoiceMessageTransactionService transactionService,
      MeterRegistry meterRegistry) {
    this.authorizationPort = authorizationPort;
    this.storagePort = storagePort;
    this.transactionService = transactionService;
    this.meterRegistry = meterRegistry;
  }

  @Override
  public SendVoiceMessageResult handle(SendVoiceMessageCommand command) {
    if (!authorizationPort.canSend(command.roomId(), command.senderId())) {
      throw new MessageRoomAccessDeniedException();
    }
    var candidateMessageId = UUID.randomUUID();
    var timer = io.micrometer.core.instrument.Timer.start(meterRegistry);
    StoredVoiceMedia stored;
    try {
      stored =
          storagePort.store(
              candidateMessageId,
              command.declaredMimeType(),
              command.declaredSizeBytes(),
              command.uploadSource());
    } catch (VoiceMediaStorageException exception) {
      stopFailedTimer(timer, command.declaredMimeType());
      if (LOGGER.isErrorEnabled()) {
        LOGGER.error(
            "voice_media_store_failed candidateMessageId={} roomId={} senderId={}"
                + " clientMessageId={}",
            candidateMessageId,
            command.roomId(),
            command.senderId(),
            command.clientMessageId(),
            exception);
      }
      throw exception;
    } catch (RuntimeException exception) {
      stopFailedTimer(timer, command.declaredMimeType());
      throw exception;
    }
    try {
      var result = transactionService.create(candidateMessageId, command, stored);
      if (!result.created()) {
        deleteCandidate(stored.storageKey(), candidateMessageId);
      }
      timer.stop(
          meterRegistry.timer(
              "synapse.voice.messages.upload",
              "outcome",
              result.created() ? "created" : "replayed",
              "mime",
              stored.mimeType()));
      meterRegistry.summary("synapse.voice.messages.bytes").record(stored.sizeBytes());
      return new SendVoiceMessageResult(result.message(), result.created());
    } catch (RuntimeException exception) {
      deleteCandidate(stored.storageKey(), candidateMessageId);
      timer.stop(
          meterRegistry.timer(
              "synapse.voice.messages.upload", "outcome", "failed", "mime", stored.mimeType()));
      throw exception;
    }
  }

  private void deleteCandidate(String storageKey, UUID candidateMessageId) {
    try {
      storagePort.delete(storageKey);
    } catch (RuntimeException cleanupFailure) {
      meterRegistry.counter("synapse.voice.messages.cleanup.failures").increment();
      if (LOGGER.isErrorEnabled()) {
        LOGGER.error(
            "voice_message_candidate_cleanup_failed candidateMessageId={}",
            candidateMessageId,
            cleanupFailure);
      }
    }
  }

  private void stopFailedTimer(io.micrometer.core.instrument.Timer.Sample timer, String mimeType) {
    timer.stop(
        meterRegistry.timer(
            "synapse.voice.messages.upload", "outcome", "failed", "mime", mimeType));
  }
}
