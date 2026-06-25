package dev.amir.synapse.identity.application.api.user_lookup;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record UserLookupResult(
    UUID userId, String displayName, @Nullable String profilePictureUrl) {}
