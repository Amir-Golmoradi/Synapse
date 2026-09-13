package dev.amir.synapse.messaging.domain.port.in.send_video_message;

@FunctionalInterface
public interface SendVideoMessageUseCase {
  SendVideoMessageResult handle(SendVideoMessageCommand command);
}
