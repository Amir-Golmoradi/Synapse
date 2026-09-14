package dev.amir.synapse.identity.domain.port.in.get_current_user;

import dev.amir.synapse.identity.domain.value_object.Email;
import org.jspecify.annotations.Nullable;

public record GetCurrentUserResult(
    String userId,
    String handle,
    Email email,
    String displayName,
    @Nullable String profilePictureUrl) {}
