package dev.amir.synapse.messaging.application.port.out;

import dev.amir.synapse.messaging.application.model.StoredMediaObject;
import dev.amir.synapse.messaging.application.model.StoredVoiceMedia;
import dev.amir.synapse.messaging.domain.port.in.send_voice_message.VoiceUploadSource;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface VoiceMediaStoragePort {
  StoredVoiceMedia store(
      UUID messageId, String declaredMimeType, long declaredSizeBytes, VoiceUploadSource source);

  InputStream open(String storageKey, long offset);

  void delete(String storageKey);

  List<StoredMediaObject> listOlderThan(Instant cutoff);
}
