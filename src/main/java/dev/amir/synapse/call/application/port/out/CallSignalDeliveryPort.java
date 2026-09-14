package dev.amir.synapse.call.application.port.out;

import dev.amir.synapse.call.application.model.CallSignal;
import java.util.UUID;

public interface CallSignalDeliveryPort {
  void sendSignal(UUID userId, String sessionId, UUID callId, UUID senderId, CallSignal signal);

  void sendInstruction(UUID userId, String sessionId, Object instruction);
}
