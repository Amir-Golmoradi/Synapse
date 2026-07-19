package dev.amir.synapse.identity.domain.service;

import dev.amir.synapse.identity.domain.exception.InvalidHandleException;
import dev.amir.synapse.identity.domain.value_object.DisplayName;
import dev.amir.synapse.identity.domain.value_object.Handle;
import dev.amir.synapse.identity.domain.value_object.UserId;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Produces the complete, deterministic candidate sequence for a user's initial Handle. */
public final class InitialHandlePolicy {
  private static final int MAX_HANDLE_LENGTH = 32;
  private static final int MAX_READABLE_SUFFIX = 5;
  private static final int UUID_BASE36_LENGTH = 25;
  private static final int PERIOD = '.';
  private static final String SYSTEM_PREFIX = "u_";
  private static final String USER_PREFIX_PROTECTION = "member_";

  public List<Handle> candidates(DisplayName displayName, UserId userId) {
    var fallback = fallbackFor(userId);
    var base = sanitize(displayName.value());
    if (base.isEmpty() || !containsAsciiLetterOrDigit(base)) {
      return List.of(fallback);
    }
    if ("u".equals(base) || base.startsWith(SYSTEM_PREFIX)) {
      base = USER_PREFIX_PROTECTION + base;
    }

    var candidates = new ArrayList<Handle>(MAX_READABLE_SUFFIX + 2);
    addIfValid(candidates, truncate(base, MAX_HANDLE_LENGTH));
    for (var suffixNumber = 1; suffixNumber <= MAX_READABLE_SUFFIX; suffixNumber++) {
      var suffix = "_" + suffixNumber;
      addIfValid(candidates, truncate(base, MAX_HANDLE_LENGTH - suffix.length()) + suffix);
    }
    candidates.add(fallback);
    return List.copyOf(candidates);
  }

  public Handle fallbackFor(UserId userId) {
    var uuid = userId.value();
    var bytes =
        ByteBuffer.allocate(16)
            .putLong(uuid.getMostSignificantBits())
            .putLong(uuid.getLeastSignificantBits())
            .array();
    var encoded = new BigInteger(1, bytes).toString(36);
    var zeroPadded = "0".repeat(UUID_BASE36_LENGTH - encoded.length()) + encoded;
    return Handle.of(SYSTEM_PREFIX + zeroPadded);
  }

  private static void addIfValid(List<Handle> candidates, String value) {
    try {
      candidates.add(Handle.of(value));
    } catch (InvalidHandleException ignored) {
      // A reserved or one-character bare base is intentionally skipped. Its suffixed variants can
      // still be readable and valid.
    }
  }

  private static String sanitize(String displayName) {
    var decomposed =
        Normalizer.normalize(displayName, Normalizer.Form.NFKD).toLowerCase(Locale.ROOT);
    var sanitized = new StringBuilder(decomposed.length());
    var previousUnderscore = false;
    var previousPeriod = false;

    var index = 0;
    while (index < decomposed.length()) {
      var codePoint = decomposed.codePointAt(index);
      index += Character.charCount(codePoint);

      if ((codePoint >= 'a' && codePoint <= 'z') || (codePoint >= '0' && codePoint <= '9')) {
        sanitized.appendCodePoint(codePoint);
        previousUnderscore = false;
        previousPeriod = false;
      } else if (codePoint == PERIOD) {
        if (!previousPeriod) {
          sanitized.append('.');
        }
        previousUnderscore = false;
        previousPeriod = true;
      } else if (isCombiningMark(codePoint)) {
        // Removing a combining mark transliterates common accented Latin names without inserting
        // a separator (for example, Jose\u0301 becomes jose).
      } else if (!previousUnderscore) {
        sanitized.append('_');
        previousUnderscore = true;
        previousPeriod = false;
      }
    }
    return stripEdgeUnderscores(sanitized.toString());
  }

  private static boolean isCombiningMark(int codePoint) {
    var type = Character.getType(codePoint);
    return type == Character.NON_SPACING_MARK
        || type == Character.COMBINING_SPACING_MARK
        || type == Character.ENCLOSING_MARK;
  }

  private static boolean containsAsciiLetterOrDigit(String value) {
    return value
        .codePoints()
        .anyMatch(
            codePoint ->
                (codePoint >= 'a' && codePoint <= 'z') || (codePoint >= '0' && codePoint <= '9'));
  }

  private static String stripEdgeUnderscores(String value) {
    var start = 0;
    var end = value.length();
    while (start < end && value.charAt(start) == '_') {
      start++;
    }
    while (end > start && value.charAt(end - 1) == '_') {
      end--;
    }
    return value.substring(start, end);
  }

  private static String truncate(String value, int maxLength) {
    return value.length() <= maxLength ? value : value.substring(0, maxLength);
  }
}
