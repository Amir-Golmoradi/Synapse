package dev.amir.synapse.messaging.domain.port.in.send_voice_message;

import java.io.IOException;
import java.io.InputStream;

@FunctionalInterface
public interface VoiceUploadSource {
  InputStream openStream() throws IOException;
}
