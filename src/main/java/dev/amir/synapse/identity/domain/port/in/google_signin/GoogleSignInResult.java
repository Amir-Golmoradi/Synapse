package dev.amir.synapse.identity.domain.port.in.google_signin;

import org.jspecify.annotations.Nullable;

public record GoogleSignInResult(
    String id,
    String handle,
    String accessToken,
    String refreshToken,
    String displayName,
    @Nullable String profilePictureUrl) {}
