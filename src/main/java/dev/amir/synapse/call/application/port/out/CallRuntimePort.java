package dev.amir.synapse.call.application.port.out;

import dev.amir.synapse.call.application.model.CallRuntimeState;
import java.util.Optional;
import java.util.UUID;

public interface CallRuntimePort {
  Optional<CallRuntimeState> findByCallId(UUID callId);

  CallRuntimeState save(CallRuntimeState state);

  void delete(UUID callId);
}
