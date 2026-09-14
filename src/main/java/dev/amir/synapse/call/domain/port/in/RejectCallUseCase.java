package dev.amir.synapse.call.domain.port.in;

import java.util.UUID;

@FunctionalInterface
public interface RejectCallUseCase {
  CallView reject(UUID callId, UUID actorId);
}
