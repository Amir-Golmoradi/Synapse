package dev.amir.synapse.call.domain.port.in;

import java.util.UUID;

@FunctionalInterface
public interface AcceptCallUseCase {
  CallView accept(UUID callId, UUID actorId, UUID clientInstanceId);
}
