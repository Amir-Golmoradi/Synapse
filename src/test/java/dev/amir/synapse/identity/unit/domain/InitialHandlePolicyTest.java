package dev.amir.synapse.identity.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import dev.amir.synapse.identity.domain.service.InitialHandlePolicy;
import dev.amir.synapse.identity.domain.value_object.DisplayName;
import dev.amir.synapse.identity.domain.value_object.Handle;
import dev.amir.synapse.identity.domain.value_object.UserId;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class InitialHandlePolicyTest {
  private final InitialHandlePolicy policy = new InitialHandlePolicy();

  @Test
  void returnsReadableCandidatesThenDeterministicUuidFallbackInExactOrder() {
    var userId = id("00000000-0000-0000-0000-000000000001");

    assertThat(policy.candidates(DisplayName.of("Amir Gm"), userId))
        .extracting(Handle::value)
        .containsExactly(
            "amir_gm",
            "amir_gm_1",
            "amir_gm_2",
            "amir_gm_3",
            "amir_gm_4",
            "amir_gm_5",
            "u_0000000000000000000000001");
  }

  @Test
  void skipsBareReservedBaseButKeepsReadableSuffixes() {
    assertThat(policy.candidates(DisplayName.of("Admin"), id(2)))
        .extracting(Handle::value)
        .startsWith("admin_1", "admin_2", "admin_3", "admin_4", "admin_5")
        .doesNotContain("admin");
  }

  @Test
  void sendsUnicodeOnlyAndPunctuationOnlyNamesDirectlyToFallback() {
    var userId = id(3);

    assertThat(policy.candidates(DisplayName.of("امیر"), userId))
        .containsExactly(policy.fallbackFor(userId));
    assertThat(policy.candidates(DisplayName.of("..."), userId))
        .containsExactly(policy.fallbackFor(userId));
  }

  @Test
  void transliteratesAccentedLatinNamesWithoutUsingEmail() {
    assertThat(policy.candidates(DisplayName.of("José"), id(4)).getFirst().value())
        .isEqualTo("jose");
  }

  @Test
  void oneCharacterAsciiNameStartsAtFirstValidSuffix() {
    assertThat(policy.candidates(DisplayName.of("A"), id(5)))
        .extracting(Handle::value)
        .startsWith("a_1", "a_2", "a_3", "a_4", "a_5");
  }

  @Test
  void protectsSystemFallbackPrefix() {
    assertThat(policy.candidates(DisplayName.of("u_member"), id(6)).getFirst().value())
        .isEqualTo("member_u_member");
  }

  @ParameterizedTest
  @ValueSource(strings = {"u", "u_", "u!"})
  void protectsSystemFallbackPrefixWhenSuffixingOneCharacterUBase(String displayName) {
    assertThat(policy.candidates(DisplayName.of(displayName), id(8)))
        .extracting(Handle::value)
        .containsExactly(
            "member_u",
            "member_u_1",
            "member_u_2",
            "member_u_3",
            "member_u_4",
            "member_u_5",
            "u_0000000000000000000000008");
  }

  @Test
  void truncatesBareAndSuffixedCandidatesSeparately() {
    var base = "abcdefghijklmnopqrstuvwxyz123456";

    assertThat(policy.candidates(DisplayName.of(base), id(7)))
        .extracting(Handle::value)
        .startsWith(base, "abcdefghijklmnopqrstuvwxyz1234_1", "abcdefghijklmnopqrstuvwxyz1234_2");
  }

  @Test
  void uuidFallbackEncodesTheCompleteUnsignedUuidInTwentyFiveBase36Digits() {
    var zero = policy.fallbackFor(id("00000000-0000-0000-0000-000000000000"));
    var maximum = policy.fallbackFor(id("ffffffff-ffff-ffff-ffff-ffffffffffff"));

    assertThat(zero.value()).isEqualTo("u_0000000000000000000000000");
    assertThat(maximum.value()).hasSize(27).startsWith("u_");
    assertThat(maximum).isEqualTo(policy.fallbackFor(id("ffffffff-ffff-ffff-ffff-ffffffffffff")));
  }

  private static UserId id(long leastSignificantBits) {
    return new UserId(new UUID(0, leastSignificantBits));
  }

  private static UserId id(String value) {
    return UserId.of(value);
  }
}
