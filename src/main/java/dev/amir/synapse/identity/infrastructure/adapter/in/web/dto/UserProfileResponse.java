package dev.amir.synapse.identity.infrastructure.adapter.in.web.dto;

import dev.amir.synapse.identity.domain.port.in.get_current_user.GetCurrentUserResult;
import org.jspecify.annotations.Nullable;

public record UserProfileResponse(
    String userId,
    String email,
    String firstName,
    String lastName,
    @Nullable String profilePictureUrl) {
  public static UserProfileResponse from(GetCurrentUserResult result) {
    return new UserProfileResponse(
        result.userId(),
        result.email(),
        result.firstName(),
        result.lastName(),
        result.profilePictureUrl());
  }
}
