package dev.amir.synapse.identity.domain.value_object;

import dev.amir.synapse.identity.domain.exception.InvalidDisplayNameException;
import dev.amir.synapse.shared.domain.ValueObject;
import java.text.Normalizer;
import java.util.Objects;

/**
 * Represents a user's presentation-layer identifier.
 *
 * <p>A DisplayName is a flexible, human-readable string used for UI rendering, social interaction,
 * and personalization. Unlike a {@link Handle}, the DisplayName does not require uniqueness and
 * allows for rich character sets, including spaces and localized scripts, to ensure a high-quality
 * user experience.
 *
 * <p>The DisplayName is normalized to a consistent Unicode form (NFKC) and validated against
 * structural and security constraints to prevent impersonation and UI-breaking artifacts.
 *
 * <p>Instances are immutable and guaranteed to be valid upon creation.
 *
 * @author Amir Golmoradi
 * @since 2026-07
 */
public record DisplayName(String value) implements ValueObject {
  private static final int MIN_LENGTH = 1;
  private static final int MAX_LENGTH = 32;

  public DisplayName {
    value = normalize(value);
    validate(value);
  }

  public static DisplayName of(String value) {
    return new DisplayName(value);
  }

  private static String normalize(String value) {
    if (Objects.isNull(value)) {
      throw InvalidDisplayNameException.missing();
    }
    String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC).strip();
    if (normalized.isEmpty()) {
      throw InvalidDisplayNameException.blank();
    }
    return normalized;
  }

  private static void validate(String normalizedValue) {
    int length = normalizedValue.codePointCount(0, normalizedValue.length());
    if (length < MIN_LENGTH || length > MAX_LENGTH) {
      throw InvalidDisplayNameException.unsupportedFormat();
    }
    if (containsForbiddenCharacters(normalizedValue)) {
      throw InvalidDisplayNameException.containsInvisibleCharacters();
    }
  }

  /**
   * Rejects non-printable control characters and invisible formatting characters.
   *
   * <p>Uses {@link String#codePoints()} to safely iterate over the full range of Unicode
   * characters, including those outside the Basic Multilingual Plane (e.g., emojis or extended
   * scripts).
   */
  private static boolean containsForbiddenCharacters(String value) {
    return value.codePoints().anyMatch(DisplayName::isControlOrFormatCharacter);
  }

  private static boolean isControlOrFormatCharacter(int cp) {
    return Character.getType(cp) == Character.CONTROL || Character.getType(cp) == Character.FORMAT;
  }

  public String getValue() {
    return value;
  }
}
