package dev.amir.synapse.messaging.domain.port.in.get_voice_media;

@FunctionalInterface
public interface GetVoiceMediaUseCase {
  VoiceMediaDownload handle(GetVoiceMediaQuery query);
}
