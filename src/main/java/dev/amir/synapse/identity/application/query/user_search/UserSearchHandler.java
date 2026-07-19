package dev.amir.synapse.identity.application.query.user_search;

import dev.amir.synapse.identity.application.port.out.user_search.UserSearchCacheLookup;
import dev.amir.synapse.identity.application.port.out.user_search.UserSearchCachePort;
import dev.amir.synapse.identity.application.port.out.user_search.UserSearchCacheToken;
import dev.amir.synapse.identity.application.port.out.user_search.UserSearchPort;
import dev.amir.synapse.identity.domain.port.in.user_search.UserSearchQuery;
import dev.amir.synapse.identity.domain.port.in.user_search.UserSearchResult;
import dev.amir.synapse.identity.domain.port.in.user_search.UserSearchUseCase;
import org.springframework.stereotype.Service;

@Service
public class UserSearchHandler implements UserSearchUseCase {
  private final UserSearchPort userSearch;
  private final UserSearchCachePort cache;

  public UserSearchHandler(UserSearchPort userSearch, UserSearchCachePort cache) {
    this.userSearch = userSearch;
    this.cache = cache;
  }

  @Override
  public UserSearchResult handle(UserSearchQuery query) {
    var lookup = getCached(query);
    if (lookup instanceof UserSearchCacheLookup.Hit hit) {
      return hit.result();
    }

    var result = userSearch.search(query);
    if (lookup instanceof UserSearchCacheLookup.Miss miss) {
      putCached(query, result, miss.token());
    }
    return result;
  }

  private UserSearchCacheLookup getCached(UserSearchQuery query) {
    try {
      return cache.get(query);
    } catch (RuntimeException ignored) {
      return new UserSearchCacheLookup.Unavailable();
    }
  }

  private void putCached(
      UserSearchQuery query, UserSearchResult result, UserSearchCacheToken token) {
    try {
      cache.put(query, result, token);
    } catch (RuntimeException ignored) {
      // Search remains PostgreSQL-backed when the optional cache is unavailable.
    }
  }
}
