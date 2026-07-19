package dev.amir.synapse.identity.infrastructure.adapter.out.oauth.google;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.amir.synapse.identity.domain.value_object.Email;
import dev.amir.synapse.identity.infrastructure.serialization.EmailJsonDeserializer;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.annotation.JsonDeserialize;

public record TokenInfoResponse(
    @Nullable String aud,
    @Nullable String iss,
    @Nullable Long exp,
    @Nullable String sub,
    @JsonDeserialize(using = EmailJsonDeserializer.class) @Nullable Email email,
    @JsonProperty("email_verified") @Nullable Boolean emailVerified,
    @Nullable String name,
    @JsonProperty("given_name") @Nullable String givenName,
    @JsonProperty("family_name") @Nullable String familyName,
    @JsonProperty("picture") @Nullable String profilePicture) {}
