package dev.amir.synapse.identity.domain.port.in.google_signin;

import org.jspecify.annotations.Nullable;

public record GoogleSignInResult(
    String id,
    String accessToken,
    String refreshToken,
    String firstName,
    String lastName,
    @Nullable String profilePictureUrl) {}
