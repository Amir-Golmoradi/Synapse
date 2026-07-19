package dev.amir.synapse.identity.infrastructure.adapter.out.persistence.user_search;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

public interface UserSearchJpaProjection {
  UUID getUserId();

  String getHandle();

  String getDisplayName();

  @Nullable String getProfilePictureUrl();
}
