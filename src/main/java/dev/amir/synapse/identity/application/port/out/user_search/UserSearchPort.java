package dev.amir.synapse.identity.application.port.out.user_search;

import dev.amir.synapse.identity.domain.port.in.user_search.UserSearchQuery;
import dev.amir.synapse.identity.domain.port.in.user_search.UserSearchResult;

@FunctionalInterface
public interface UserSearchPort {
  UserSearchResult search(UserSearchQuery query);
}
