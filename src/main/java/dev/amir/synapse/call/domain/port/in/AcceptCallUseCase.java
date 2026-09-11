package dev.amir.synapse.call.domain.port.in;

import java.util.UUID;

public interface AcceptCallUseCase {
  CallView accept(UUID callId, UUID actorId, UUID clientInstanceId);
}
