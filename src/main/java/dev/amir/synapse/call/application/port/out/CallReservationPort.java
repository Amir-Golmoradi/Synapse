package dev.amir.synapse.call.application.port.out;

import java.util.UUID;

public interface CallReservationPort {
  void reserve(UUID callId, UUID callerId, UUID calleeId);

  void release(UUID callId);
}
