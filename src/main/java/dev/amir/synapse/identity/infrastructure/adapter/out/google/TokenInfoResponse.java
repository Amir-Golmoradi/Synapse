package dev.amir.synapse.identity.infrastructure.adapter.out.google;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

public record TokenInfoResponse(
    @Nullable String aud,
    @Nullable String iss,
    @Nullable Long exp,
    @Nullable String sub,
    @Nullable String email,
    @JsonProperty("email_verified") @Nullable Boolean emailVerified,
    @JsonProperty("given_name") @Nullable String givenName,
    @JsonProperty("family_name") @Nullable String familyName,
    @JsonProperty("picture") @Nullable String profilePicture) {}
