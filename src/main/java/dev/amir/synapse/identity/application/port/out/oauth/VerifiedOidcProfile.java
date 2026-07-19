package dev.amir.synapse.identity.application.port.out.oauth;

import dev.amir.synapse.identity.domain.value_object.DisplayName;
import dev.amir.synapse.identity.domain.value_object.Email;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** Provider-neutral identity claims that have already passed cryptographic/provider validation. */
public record VerifiedOidcProfile(
    String provider,
    String subjectId,
    Email email,
    DisplayName displayName,
    @Nullable String profilePictureUrl) {
  public VerifiedOidcProfile {
    provider = Objects.requireNonNull(provider, "provider");
    subjectId = Objects.requireNonNull(subjectId, "subjectId");
    email = Objects.requireNonNull(email, "email");
    displayName = Objects.requireNonNull(displayName, "displayName");
  }
}
