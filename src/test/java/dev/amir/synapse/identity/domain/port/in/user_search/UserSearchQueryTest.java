package dev.amir.synapse.identity.domain.port.in.user_search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.amir.synapse.identity.domain.exception.InvalidIdentityRequestException;
import org.junit.jupiter.api.Test;

class UserSearchQueryTest {

  @Test
  void normalizesPrefixToLowercase() {
    var query = new UserSearchQuery("Ami._9", 2, 20);

    assertThat(query.prefix()).isEqualTo("ami._9");
    assertThat(query.page()).isEqualTo(2);
    assertThat(query.size()).isEqualTo(20);
  }

  @Test
  void acceptsPrefixLengthBoundariesAndHandleAlphabet() {
    assertThat(new UserSearchQuery("a", 0, 1).prefix()).isEqualTo("a");
    assertThat(new UserSearchQuery("a2345678901234567890123456789b._", 0, 100).prefix())
        .hasSize(32)
        .containsOnlyOnce(".")
        .containsOnlyOnce("_");
  }

  @Test
  void rejectsMissingOrEmptyPrefix() {
    assertInvalidPrefix(null);
    assertInvalidPrefix("");
  }

  @Test
  void rejectsPrefixLongerThanThirtyTwoCharacters() {
    assertInvalidPrefix("a".repeat(33));
  }

  @Test
  void rejectsCharactersOutsideHandleAlphabet() {
    assertInvalidPrefix("ami-r");
    assertInvalidPrefix("ami r");
    assertInvalidPrefix(" ami");
    assertInvalidPrefix("amir@");
    assertInvalidPrefix("امیر");
  }

  @Test
  void rejectsConsecutivePeriods() {
    assertInvalidPrefix("ami..r");
  }

  @Test
  void rejectsNegativePage() {
    assertThatThrownBy(() -> new UserSearchQuery("ami", -1, 20))
        .isInstanceOf(InvalidIdentityRequestException.class);
  }

  @Test
  void rejectsNonPositiveSize() {
    assertThatThrownBy(() -> new UserSearchQuery("ami", 0, 0))
        .isInstanceOf(InvalidIdentityRequestException.class);
    assertThatThrownBy(() -> new UserSearchQuery("ami", 0, -1))
        .isInstanceOf(InvalidIdentityRequestException.class);
  }

  @Test
  void rejectsSizeAboveOneHundred() {
    assertThatThrownBy(() -> new UserSearchQuery("ami", 0, 101))
        .isInstanceOf(InvalidIdentityRequestException.class);
  }

  private static void assertInvalidPrefix(String prefix) {
    assertThatThrownBy(() -> new UserSearchQuery(prefix, 0, 20))
        .isInstanceOf(InvalidIdentityRequestException.class);
  }
}
