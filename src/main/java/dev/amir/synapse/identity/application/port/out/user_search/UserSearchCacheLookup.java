package dev.amir.synapse.identity.application.port.out.user_search;

import dev.amir.synapse.identity.domain.port.in.user_search.UserSearchResult;
import java.util.Objects;

/** Result of a fail-open cache lookup, including a write token only for a genuine miss. */
public sealed interface UserSearchCacheLookup {
  record Hit(UserSearchResult result) implements UserSearchCacheLookup {
    public Hit {
      Objects.requireNonNull(result, "Cached search result cannot be null");
    }
  }

  record Miss(UserSearchCacheToken token) implements UserSearchCacheLookup {
    public Miss {
      Objects.requireNonNull(token, "Cache token cannot be null");
    }
  }

  record Unavailable() implements UserSearchCacheLookup {}
}
