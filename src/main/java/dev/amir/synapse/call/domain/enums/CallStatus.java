package dev.amir.synapse.call.domain.enums;

public enum CallStatus {
  RINGING,
  CONNECTING,
  ACTIVE,
  RECOVERING,
  REJECTED,
  CANCELLED,
  MISSED,
  ENDED,
  FAILED;

  public boolean isTerminal() {
    return switch (this) {
      case REJECTED, CANCELLED, MISSED, ENDED, FAILED -> true;
      default -> false;
    };
  }

  public boolean isAccepted() {
    return this == CONNECTING
        || this == ACTIVE
        || this == RECOVERING
        || this == ENDED
        || this == FAILED;
  }
}
