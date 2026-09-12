package dev.amir.synapse.messaging.application.query.get_voice_media;

import dev.amir.synapse.messaging.application.port.out.VoiceMediaStoragePort;
import dev.amir.synapse.messaging.domain.exception.InvalidMediaRangeException;
import dev.amir.synapse.messaging.domain.exception.MessageMediaNotFoundException;
import dev.amir.synapse.messaging.domain.port.in.get_voice_media.GetVoiceMediaQuery;
import dev.amir.synapse.messaging.domain.port.in.get_voice_media.GetVoiceMediaUseCase;
import dev.amir.synapse.messaging.domain.port.in.get_voice_media.VoiceMediaDownload;
import dev.amir.synapse.messaging.domain.port.out.VoiceMessageMediaPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetVoiceMediaHandler implements GetVoiceMediaUseCase {
  private final VoiceMessageMediaPort mediaPort;
  private final VoiceMediaStoragePort storagePort;

  public GetVoiceMediaHandler(VoiceMessageMediaPort mediaPort, VoiceMediaStoragePort storagePort) {
    this.mediaPort = mediaPort;
    this.storagePort = storagePort;
  }

  @Override
  @Transactional(readOnly = true)
  public VoiceMediaDownload handle(GetVoiceMediaQuery query) {
    var media =
        mediaPort
            .findAuthorized(query.roomId(), query.messageId(), query.requesterId())
            .orElseThrow(MessageMediaNotFoundException::new);
    var total = media.sizeBytes();
    long offset = 0;
    long length = total;
    var partial = query.range() != null;
    if (query.range() != null) {
      var range = query.range();
      if (range.suffixLength() != null) {
        length = Math.min(range.suffixLength(), total);
        offset = total - length;
      } else {
        offset = range.start();
        if (offset >= total) {
          throw new InvalidMediaRangeException();
        }
        var last = range.end() == null ? total - 1 : Math.min(range.end(), total - 1);
        length = last - offset + 1;
      }
    }
    return new VoiceMediaDownload(
        storagePort.open(media.storageKey(), offset),
        media.mimeType(),
        total,
        offset,
        length,
        partial);
  }
}
