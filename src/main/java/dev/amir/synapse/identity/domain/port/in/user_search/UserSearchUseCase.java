package dev.amir.synapse.identity.domain.port.in.user_search;

@FunctionalInterface
public interface UserSearchUseCase {
  UserSearchResult handle(UserSearchQuery query);
}
