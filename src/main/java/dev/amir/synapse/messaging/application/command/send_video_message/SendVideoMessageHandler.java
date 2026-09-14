package dev.amir.synapse.messaging.application.command.send_video_message;

import dev.amir.synapse.messaging.application.model.VideoMessageSettings;
import dev.amir.synapse.messaging.application.service.VideoMessageTransactionService;
import dev.amir.synapse.messaging.domain.exception.MessageRoomAccessDeniedException;
import dev.amir.synapse.messaging.domain.exception.VideoMessageException;
import dev.amir.synapse.messaging.domain.port.in.send_video_message.SendVideoMessageCommand;
import dev.amir.synapse.messaging.domain.port.in.send_video_message.SendVideoMessageResult;
import dev.amir.synapse.messaging.domain.port.in.send_video_message.SendVideoMessageUseCase;
import dev.amir.synapse.messaging.domain.port.out.MessageAuthorizationPort;
import dev.amir.synapse.messaging.domain.port.out.MessageMediaStoragePort;
import dev.amir.synapse.messaging.domain.port.out.MessageMediaStoragePort.StagedMedia;
import dev.amir.synapse.messaging.domain.port.out.VideoMediaInspectionPort;
import dev.amir.synapse.messaging.domain.value_object.VideoMetadata;
import dev.amir.synapse.shared.domain.DomainException;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SendVideoMessageHandler implements SendVideoMessageUseCase {
  private static final Logger LOGGER = LoggerFactory.getLogger(SendVideoMessageHandler.class);

  private final MessageAuthorizationPort authorization;
  private final MessageMediaStoragePort storage;
  private final VideoMediaInspectionPort inspection;
  private final VideoMessageTransactionService transaction;
  private final VideoMessageSettings settings;
  private final MeterRegistry meterRegistry;

  public SendVideoMessageHandler(
      MessageAuthorizationPort authorization,
      MessageMediaStoragePort storage,
      VideoMediaInspectionPort inspection,
      VideoMessageTransactionService transaction,
      VideoMessageSettings settings,
      MeterRegistry meterRegistry) {
    this.authorization = authorization;
    this.storage = storage;
    this.inspection = inspection;
    this.transaction = transaction;
    this.settings = settings;
    this.meterRegistry = meterRegistry;
  }

  @Override
  public SendVideoMessageResult handle(SendVideoMessageCommand command) {
    var startedAt = System.nanoTime();
    try {
      var result = process(command);
      meterRegistry
          .counter(
              "synapse.messaging.video.uploads", "outcome", result.created() ? "created" : "retry")
          .increment();
      var media = Objects.requireNonNull(result.message().media());
      meterRegistry.summary("synapse.messaging.video.upload.bytes").record(media.sizeBytes());
      meterRegistry.summary("synapse.messaging.video.duration").record(media.durationMs());
      return result;
    } catch (RuntimeException exception) {
      var reason =
          exception instanceof DomainException domain ? domain.getErrorCode() : "INTERNAL_ERROR";
      meterRegistry
          .counter("synapse.messaging.video.uploads", "outcome", "failure", "reason", reason)
          .increment();
      throw exception;
    } finally {
      meterRegistry
          .timer("synapse.messaging.video.upload.latency")
          .record(System.nanoTime() - startedAt, TimeUnit.NANOSECONDS);
    }
  }

  private SendVideoMessageResult process(SendVideoMessageCommand command) {
    if (command.declaredSize() > settings.maxFileSize()) {
      throw VideoMessageException.tooLarge();
    }
    if (!authorization.canSend(command.roomId(), command.senderId())) {
      throw new MessageRoomAccessDeniedException();
    }

    StagedMedia staged = null;
    String storageKey = null;
    try {
      try (var content = command.content().open()) {
        staged = storage.stage(content, settings.maxFileSize());
      }
      var metadata =
          inspection.inspect(staged.path(), command.declaredContentType(), staged.sizeBytes());
      validateLimits(metadata);
      storageKey = storage.promote(staged, metadata.contentType());
      var persisted =
          transaction.create(
              command.roomId(),
              command.senderId(),
              command.clientMessageId(),
              storageKey,
              staged.sha256(),
              metadata);
      if (!persisted.created()) {
        safeDelete(storageKey);
      }
      return new SendVideoMessageResult(persisted.message(), persisted.created());
    } catch (IOException exception) {
      throw VideoMessageException.unavailable("The video upload could not be read.", exception);
    } catch (RuntimeException exception) {
      if (storageKey != null) {
        safeDelete(storageKey);
      }
      throw exception;
    } finally {
      if (staged != null) {
        safeDiscard(staged);
      }
    }
  }

  private void validateLimits(VideoMetadata media) {
    if (media.durationMs() > settings.maxDuration().toMillis()) {
      throw VideoMessageException.durationExceeded();
    }
    if (media.width() > settings.maxDimension()
        || media.height() > settings.maxDimension()
        || (long) media.width() * media.height() > settings.maxPixels()) {
      throw VideoMessageException.dimensionsExceeded();
    }
  }

  private void safeDelete(String storageKey) {
    try {
      storage.delete(storageKey);
    } catch (RuntimeException exception) {
      if (LOGGER.isErrorEnabled()) {
        LOGGER.error("video_media_compensation_failed", exception);
      }
    }
  }

  private void safeDiscard(StagedMedia staged) {
    try {
      storage.discard(staged);
    } catch (RuntimeException exception) {
      if (LOGGER.isErrorEnabled()) {
        LOGGER.error("video_media_staging_cleanup_failed", exception);
      }
    }
  }
}
