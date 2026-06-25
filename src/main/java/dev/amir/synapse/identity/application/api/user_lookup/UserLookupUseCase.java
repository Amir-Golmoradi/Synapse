package dev.amir.synapse.identity.application.api.user_lookup;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface UserLookupUseCase {

  boolean existsByUserId(UUID userId);

  UserLookupResult getRequiredUserById(UUID userId);

  Map<UUID, UserLookupResult> getUsersByIds(Set<UUID> userIds);
}
