package dev.amir.synapse.call.domain.port.in;

import java.util.UUID;

public interface EndCallUseCase {
  CallView end(UUID callId, UUID actorId, EndReason reason);

  enum EndReason {
    HANGUP,
    MEDIA_ERROR
  }
}
