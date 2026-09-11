package dev.amir.synapse.call.domain.port.in;

import java.util.UUID;

public interface ResumeCallUseCase {
  CallView resume(Command command);

  record Command(
      UUID callId,
      UUID actorId,
      UUID clientInstanceId,
      UUID requestId,
      int observedGeneration,
      boolean peerConnectionRetained) {}
}
