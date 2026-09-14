package dev.amir.synapse.identity.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.amir.synapse.identity.domain.exception.InvalidHandleException;
import dev.amir.synapse.identity.domain.value_object.Handle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class HandleTest {
  @Test
  void normalizesCaseAndSurroundingWhitespace() {
    assertThat(Handle.of(" Amir.GM_ ").value()).isEqualTo("amir.gm_");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "ab",
        "1a",
        "_a",
        "a_",
        ".a",
        "a.",
        "a__b",
        "a_b.c",
        "abcdefghijklmnopqrstuvwxyz123456"
      })
  void acceptsBroadHandleGrammar(String value) {
    assertThat(Handle.of(value).value()).isEqualTo(value);
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " ", "a", "a..b", "a-b", "a@b", "abcdefghijklmnopqrstuvwxyz1234567"})
  void rejectsValuesOutsideBroadGrammar(String value) {
    assertThatThrownBy(() -> Handle.of(value)).isInstanceOf(InvalidHandleException.class);
  }

  @Test
  void rejectsNull() {
    assertThatThrownBy(() -> Handle.of(null)).isInstanceOf(InvalidHandleException.class);
  }

  @ParameterizedTest
  @ValueSource(strings = {"admin", "root", "system", "null", "user", "api"})
  void rejectsExactReservedNames(String value) {
    assertThatThrownBy(() -> Handle.of(value)).isInstanceOf(InvalidHandleException.class);
  }

  @ParameterizedTest
  @ValueSource(strings = {"admin_1", "admins", "user_1", "apiary"})
  void allowsNonExactReservedVariants(String value) {
    assertThat(Handle.of(value).value()).isEqualTo(value);
  }
}
