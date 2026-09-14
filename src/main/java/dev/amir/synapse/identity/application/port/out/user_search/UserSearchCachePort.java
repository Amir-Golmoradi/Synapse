package dev.amir.synapse.identity.application.port.out.user_search;

import dev.amir.synapse.identity.domain.port.in.user_search.UserSearchQuery;
import dev.amir.synapse.identity.domain.port.in.user_search.UserSearchResult;

/** Optional performance layer. Implementations must never make identity operations depend on it. */
public interface UserSearchCachePort {
  UserSearchCacheLookup get(UserSearchQuery query);

  void put(UserSearchQuery query, UserSearchResult result, UserSearchCacheToken token);

  void incrementGeneration();
}
