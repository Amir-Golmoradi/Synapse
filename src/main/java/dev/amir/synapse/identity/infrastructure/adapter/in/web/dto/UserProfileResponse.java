package dev.amir.synapse.identity.infrastructure.adapter.in.web.dto;

import dev.amir.synapse.identity.domain.port.in.get_current_user.GetCurrentUserResult;
import dev.amir.synapse.identity.domain.value_object.Email;
import dev.amir.synapse.identity.infrastructure.serialization.EmailJsonSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.annotation.JsonSerialize;

public record UserProfileResponse(
    String userId,
    String handle,
    @JsonSerialize(using = EmailJsonSerializer.class) @Schema(type = "string", format = "email")
        Email email,
    String displayName,
    @Nullable String profilePictureUrl) {
  public static UserProfileResponse from(GetCurrentUserResult result) {
    return new UserProfileResponse(
        result.userId(),
        result.handle(),
        result.email(),
        result.displayName(),
        result.profilePictureUrl());
  }
}
