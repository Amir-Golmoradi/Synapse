package dev.amir.synapse.identity.infrastructure.adapter.out.persistence.user_search;

import dev.amir.synapse.identity.application.port.out.user_search.UserSearchPort;
import dev.amir.synapse.identity.domain.port.in.user_search.UserSearchItem;
import dev.amir.synapse.identity.domain.port.in.user_search.UserSearchQuery;
import dev.amir.synapse.identity.domain.port.in.user_search.UserSearchResult;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class UserSearchJpaAdapter implements UserSearchPort {
  private final UserSearchJpaRepository repository;

  public UserSearchJpaAdapter(UserSearchJpaRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional(readOnly = true)
  public UserSearchResult search(UserSearchQuery query) {
    int requestedSize = query.size();
    int limit = Math.addExact(requestedSize, 1);
    long offset = Math.multiplyExact((long) query.page(), requestedSize);
    var rows =
        repository.searchByHandlePrefix(escapeLikePrefix(query.prefix()) + "%", limit, offset);
    boolean hasNext = rows.size() > requestedSize;
    int resultSize = Math.min(rows.size(), requestedSize);
    List<UserSearchItem> items =
        rows.subList(0, resultSize).stream().map(UserSearchJpaAdapter::toItem).toList();

    return new UserSearchResult(items, query.page(), requestedSize, hasNext);
  }

  static String escapeLikePrefix(String prefix) {
    return prefix.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }

  private static UserSearchItem toItem(UserSearchJpaProjection projection) {
    return new UserSearchItem(
        projection.getUserId(),
        projection.getHandle(),
        projection.getDisplayName(),
        projection.getProfilePictureUrl());
  }
}
