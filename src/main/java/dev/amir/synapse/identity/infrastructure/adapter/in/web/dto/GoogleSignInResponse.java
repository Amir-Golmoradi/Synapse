package dev.amir.synapse.identity.infrastructure.adapter.in.web.dto;

import dev.amir.synapse.identity.domain.port.in.google_signin.GoogleSignInResult;
import org.jspecify.annotations.Nullable;

public record GoogleSignInResponse(
    String userId,
    String handle,
    String accessToken,
    String refreshToken,
    String displayName,
    @Nullable String profilePictureUrl) {
  public static GoogleSignInResponse from(GoogleSignInResult result) {
    return new GoogleSignInResponse(
        result.id(),
        result.handle(),
        result.accessToken(),
        result.refreshToken(),
        result.displayName(),
        result.profilePictureUrl());
  }
}
