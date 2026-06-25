package dev.amir.synapse.identity.infrastructure.adapter.out.persistence.refresh_token;

import dev.amir.synapse.identity.application.port.out.refresh_token.LoadRefreshTokenPort;
import dev.amir.synapse.identity.application.port.out.refresh_token.RevokeRefreshTokenPort;
import dev.amir.synapse.identity.application.port.out.refresh_token.SaveRefreshTokenPort;
import dev.amir.synapse.identity.domain.entity.RefreshToken;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class RefreshTokenJpaAdapter
    implements SaveRefreshTokenPort, LoadRefreshTokenPort, RevokeRefreshTokenPort {

  private final RefreshTokenJpaRepository repository;
  private final RefreshTokenMapper mapper;

  public RefreshTokenJpaAdapter(RefreshTokenJpaRepository repository, RefreshTokenMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public void save(RefreshToken token) {
    repository.save(mapper.toEntity(token));
  }

  @Override
  public Optional<RefreshToken> findByTokenHash(String tokenHash) {
    return repository.findByTokenHash(tokenHash).map(mapper::toDomain);
  }
}
