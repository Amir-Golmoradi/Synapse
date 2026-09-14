package dev.amir.synapse.identity.infrastructure.adapter.in.web.dto;

import dev.amir.synapse.identity.domain.port.in.user_search.UserSearchItem;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record UserSearchItemResponse(
    UUID userId, String handle, String displayName, @Nullable String profilePictureUrl) {
  static UserSearchItemResponse from(UserSearchItem item) {
    return new UserSearchItemResponse(
        item.userId(), item.handle(), item.displayName(), item.profilePictureUrl());
  }
}
