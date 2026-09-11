package dev.amir.synapse.call.application.model;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record CallSignal(
    UUID messageId,
    int generation,
    Type type,
    @Nullable String sdp,
    @Nullable String candidate,
    @Nullable String sdpMid,
    @Nullable Integer sdpMLineIndex,
    @Nullable String usernameFragment,
    @Nullable UUID descriptionId) {
  public enum Type {
    OFFER,
    ANSWER,
    ICE_CANDIDATE
  }
}
