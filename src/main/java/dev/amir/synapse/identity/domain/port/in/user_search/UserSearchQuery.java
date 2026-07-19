package dev.amir.synapse.identity.domain.port.in.user_search;

import dev.amir.synapse.identity.domain.exception.InvalidIdentityRequestException;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public record UserSearchQuery(String prefix, int page, int size) {
  public static final int DEFAULT_PAGE_SIZE = 20;
  public static final int MAX_PAGE_SIZE = 100;

  private static final Pattern VALID_PREFIX = Pattern.compile("^(?!.*\\.\\.)[a-z0-9._]{1,32}$");

  public UserSearchQuery {
    if (Objects.isNull(prefix)) {
      throw invalid("Search prefix is required.");
    }

    prefix = prefix.toLowerCase(Locale.ROOT);
    if (!VALID_PREFIX.matcher(prefix).matches()) {
      throw invalid(
          "Search prefix must contain 1 to 32 letters, numbers, periods, or underscores and cannot contain consecutive periods.");
    }
    if (page < 0) {
      throw invalid("Search page cannot be negative.");
    }
    if (size < 1 || size > MAX_PAGE_SIZE) {
      throw invalid("Search page size must be between 1 and " + MAX_PAGE_SIZE + ".");
    }
  }

  private static InvalidIdentityRequestException invalid(String message) {
    return new InvalidIdentityRequestException(message, new IllegalArgumentException(message));
  }
}
