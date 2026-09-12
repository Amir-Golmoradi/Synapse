package dev.amir.synapse.messaging.domain.port.out;

@FunctionalInterface
public interface VoiceMessageWritePort {
  VoiceMessageWriteResult saveAuthorized(VoiceMessageWriteRequest request);
}
