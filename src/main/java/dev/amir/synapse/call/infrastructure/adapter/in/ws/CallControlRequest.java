package dev.amir.synapse.call.infrastructure.adapter.in.ws;

import java.util.UUID;

public record CallControlRequest(
    UUID clientInstanceId,
    int generation,
    Type type,
    UUID requestId,
    boolean peerConnectionRetained) {
  public enum Type {
    READY,
    CONNECTED,
    HEARTBEAT,
    RECOVERY_REQUIRED
  }
}
