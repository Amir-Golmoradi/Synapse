package dev.amir.synapse.identity.infrastructure.adapter.in.web.dto;

import dev.amir.synapse.identity.domain.port.in.user_search.UserSearchResult;
import java.util.List;

public record UserSearchResponse(
    List<UserSearchItemResponse> items, int page, int size, boolean hasNext) {
  public UserSearchResponse {
    items = List.copyOf(items);
  }

  public static UserSearchResponse from(UserSearchResult result) {
    return new UserSearchResponse(
        result.items().stream().map(UserSearchItemResponse::from).toList(),
        result.page(),
        result.size(),
        result.hasNext());
  }
}
