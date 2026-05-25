package dev.amir.synapse.identity.domain.model;

import dev.amir.synapse.identity.domain.value_object.Email;
import dev.amir.synapse.identity.domain.value_object.FullName;
import org.jspecify.annotations.Nullable;

public record GoogleUserInfo(
    String googleId, Email email, FullName fullName, @Nullable String profilePictureUrl) {}
