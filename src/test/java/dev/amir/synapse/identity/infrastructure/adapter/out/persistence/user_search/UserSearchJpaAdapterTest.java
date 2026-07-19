package dev.amir.synapse.identity.infrastructure.adapter.out.persistence.user_search;

import static org.assertj.core.api.Assertions.assertThat;

import dev.amir.synapse.identity.domain.port.in.user_search.UserSearchQuery;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class UserSearchJpaAdapterTest {

  @Test
  void escapesLiteralUnderscoreAndUsesLimitPlusOneWithPageSizeOffset() {
    var pattern = new AtomicReference<String>();
    var limit = new AtomicInteger();
    var offset = new AtomicLong();
    var rows =
        List.of(
            projection("ami_r", "Amir"),
            projection("ami_ra", "Amira"),
            projection("ami_rb", "Amir B"));
    UserSearchJpaRepository repository =
        (prefixPattern, queryLimit, queryOffset) -> {
          pattern.set(prefixPattern);
          limit.set(queryLimit);
          offset.set(queryOffset);
          return rows;
        };
    var adapter = new UserSearchJpaAdapter(repository);

    var result = adapter.search(new UserSearchQuery("AMI_R", 2, 2));

    assertThat(pattern.get()).isEqualTo("ami\\_r%");
    assertThat(limit.get()).isEqualTo(3);
    assertThat(offset.get()).isEqualTo(4);
    assertThat(result.items()).extracting(item -> item.handle()).containsExactly("ami_r", "ami_ra");
    assertThat(result.page()).isEqualTo(2);
    assertThat(result.size()).isEqualTo(2);
    assertThat(result.hasNext()).isTrue();
  }

  @Test
  void escapesEveryJpaLikeMetacharacter() {
    assertThat(UserSearchJpaAdapter.escapeLikePrefix("a_b%\\c")).isEqualTo("a\\_b\\%\\\\c");
  }

  private static UserSearchJpaProjection projection(String handle, String displayName) {
    var userId = UUID.randomUUID();
    return new UserSearchJpaProjection() {
      @Override
      public UUID getUserId() {
        return userId;
      }

      @Override
      public String getHandle() {
        return handle;
      }

      @Override
      public String getDisplayName() {
        return displayName;
      }

      @Override
      public String getProfilePictureUrl() {
        return null;
      }
    };
  }
}
