package dev.amir.synapse.identity.domain.port.in.user_search;

import java.util.List;
import java.util.Objects;

public record UserSearchResult(List<UserSearchItem> items, int page, int size, boolean hasNext) {
  private static final int MIN_PAGE_SIZE = 1;

  public UserSearchResult {
    Objects.requireNonNull(items, "Search items cannot be null");
    if (page < 0) {
      throw new IllegalArgumentException("Search page cannot be negative.");
    }
    if (size < MIN_PAGE_SIZE) {
      throw new IllegalArgumentException("Search page size must be positive.");
    }
    items = List.copyOf(items);
  }
}
