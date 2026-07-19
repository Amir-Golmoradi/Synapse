package dev.amir.synapse.identity.domain.value_object;

import dev.amir.synapse.identity.domain.exception.InvalidHandleException;
import dev.amir.synapse.shared.domain.ValueObject;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Represents a user's stable public identifier.
 *
 * <p>A Handle is a unique, human-readable identifier used to reference users throughout the
 * platform. Unlike email addresses or external identity provider identifiers, Handles are safe to
 * expose publicly and are intended to remain stable over time.
 *
 * <p>Lowercase letters, digits, underscore ({@code _}), and period ({@code .}) are permitted,
 * length is 2-32 characters, and the only forbidden sequence is two consecutive periods. There is
 * no requirement to start with a letter.
 *
 * <p>Examples:
 *
 * <ul>
 *   <li>amir
 *   <li>amir_123
 *   <li>john_doe
 *   <li>j.doe
 * </ul>
 *
 * <p>Instances are immutable and always stored in normalized form.
 *
 * @author Amir Golmoradi
 * @since 2026-10-07
 */
public record Handle(String value) implements ValueObject {
  private static final Pattern VALID_PATTERN = Pattern.compile("^(?!.*\\.\\.)[a-z0-9._]{2,32}$");
  private static final Set<String> RESERVED_HANDLES =
      Set.of(
          "admin",
          "administrator",
          "api",
          "auth",
          "cdn",
          "channel",
          "channels",
          "everyone",
          "help",
          "here",
          "login",
          "logout",
          "me",
          "messages",
          "null",
          "platform",
          "profile",
          "profiles",
          "register",
          "root",
          "security",
          "server",
          "servers",
          "settings",
          "signin",
          "signup",
          "static",
          "support",
          "synapse",
          "system",
          "terms",
          "undefined",
          "user",
          "users");

  public Handle {
    value = normalize(value);
    validate(value);
  }

  public static Handle of(String value) {
    return new Handle(value);
  }

  private static String normalize(String value) {
    return value == null ? null : value.strip().toLowerCase(Locale.ROOT);
  }

  private static void validate(String value) {
    if (Objects.isNull(value)) {
      throw InvalidHandleException.missing();
    }
    if (!VALID_PATTERN.matcher(value).matches()) {
      throw InvalidHandleException.unsupportedFormat();
    }

    if (RESERVED_HANDLES.contains(value)) {
      throw InvalidHandleException.reserved(value);
    }
  }
}
