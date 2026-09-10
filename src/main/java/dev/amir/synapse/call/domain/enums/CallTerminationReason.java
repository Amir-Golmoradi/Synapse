package dev.amir.synapse.call.domain.enums;

public enum CallTerminationReason {
  REJECTED,
  CALLER_CANCELLED,
  NO_ANSWER,
  HANGUP,
  MEDIA_ERROR,
  CONNECTION_TIMEOUT,
  PARTICIPANT_UNREACHABLE
}
