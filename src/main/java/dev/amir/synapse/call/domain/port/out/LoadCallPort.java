package dev.amir.synapse.call.domain.port.out;

import dev.amir.synapse.call.domain.model.Call;
import java.util.Optional;
import java.util.UUID;

public interface LoadCallPort {
  Optional<Call> findById(UUID callId);

  Optional<Call> findByIdForUpdate(UUID callId);

  Optional<Call> findByCallerAndRequestId(UUID callerId, UUID clientRequestId);

  Optional<Call> findCurrentByParticipant(UUID userId);
}
