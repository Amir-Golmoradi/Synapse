package dev.amir.synapse.identity.domain.port.in.get_current_user;

import org.jspecify.annotations.Nullable;

public record GetCurrentUserResult(
    String userId,
    String email,
    String firstName,
    String lastName,
    @Nullable String profilePictureUrl) {}
