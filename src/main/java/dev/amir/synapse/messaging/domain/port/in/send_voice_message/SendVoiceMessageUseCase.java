package dev.amir.synapse.messaging.domain.port.in.send_voice_message;

@FunctionalInterface
public interface SendVoiceMessageUseCase {
  SendVoiceMessageResult handle(SendVoiceMessageCommand command);
}
