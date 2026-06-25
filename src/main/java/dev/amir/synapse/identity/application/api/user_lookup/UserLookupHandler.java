package dev.amir.synapse.identity.application.api.user_lookup;

import dev.amir.synapse.identity.application.port.out.user.LoadUserPort;
import dev.amir.synapse.identity.domain.value_object.UserId;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserLookupHandler implements UserLookupUseCase {
  private final LoadUserPort loadUserPort;
  private final UserLookupMapper lookupMapper;

  public UserLookupHandler(LoadUserPort loadUserPort, UserLookupMapper lookupMapper) {
    this.loadUserPort = loadUserPort;
    this.lookupMapper = lookupMapper;
  }

  @Transactional(readOnly = true)
  @Override
  public boolean existsByUserId(UUID userId) {
    return loadUserPort.findById(new UserId(userId)).isPresent();
  }

  @Transactional(readOnly = true)
  @Override
  public UserLookupResult getRequiredUserById(UUID userId) {
    return loadUserPort.findById(new UserId(userId)).map(lookupMapper).orElseThrow();
  }

  @Transactional(readOnly = true)
  @Override
  public Map<UUID, UserLookupResult> getUsersByIds(Set<UUID> userIds) {
    return userIds.stream()
        .map(id -> loadUserPort.findById(new UserId(id)))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toMap(u -> u.getId().getValue(), lookupMapper));
  }
}
