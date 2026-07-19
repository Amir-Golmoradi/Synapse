package dev.amir.synapse.identity.domain.port.in.user_search;

import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record UserSearchItem(
    UUID userId, String handle, String displayName, @Nullable String profilePictureUrl) {
  public UserSearchItem {
    Objects.requireNonNull(userId, "User ID cannot be null");
    Objects.requireNonNull(handle, "Handle cannot be null");
    Objects.requireNonNull(displayName, "Display name cannot be null");
  }
}
