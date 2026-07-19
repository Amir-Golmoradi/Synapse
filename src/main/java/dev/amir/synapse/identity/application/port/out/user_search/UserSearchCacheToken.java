package dev.amir.synapse.identity.application.port.out.user_search;

import java.util.Objects;

/** Opaque cache version observed before loading a search slice from PostgreSQL. */
public record UserSearchCacheToken(String value) {
  public UserSearchCacheToken {
    Objects.requireNonNull(value, "Cache token cannot be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException("Cache token cannot be blank.");
    }
  }
}
