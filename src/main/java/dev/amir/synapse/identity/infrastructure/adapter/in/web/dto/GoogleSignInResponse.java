package dev.amir.synapse.identity.infrastructure.adapter.in.web.dto;

import dev.amir.synapse.identity.domain.port.in.google_signin.GoogleSignInResult;
import org.jspecify.annotations.Nullable;

public record GoogleSignInResponse(
    String userId,
    String accessToken,
    String refreshToken,
    String firstName,
    String lastName,
    @Nullable String profilePictureUrl) {
  public static GoogleSignInResponse from(GoogleSignInResult result) {
    return new GoogleSignInResponse(
        result.id(),
        result.accessToken(),
        result.refreshToken(),
        result.firstName(),
        result.lastName(),
        result.profilePictureUrl());
  }
}
