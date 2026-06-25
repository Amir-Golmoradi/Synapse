package dev.amir.synapse.identity.domain.value_object;

import org.jspecify.annotations.Nullable;

public record GoogleUserInfo(
    String googleId, Email email, FullName fullName, @Nullable String profilePictureUrl) {}
