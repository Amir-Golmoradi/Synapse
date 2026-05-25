package dev.amir.synapse.identity.infrastructure.adapter.in.persistence.refresh_token;

import dev.amir.synapse.identity.domain.model.RefreshToken;
import dev.amir.synapse.identity.domain.port.out.refresh_token.LoadRefreshTokenPort;
import dev.amir.synapse.identity.domain.port.out.refresh_token.RevokeRefreshTokenPort;
import dev.amir.synapse.identity.domain.port.out.refresh_token.SaveRefreshTokenPort;
import dev.amir.synapse.identity.domain.value_object.UserId;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class RefreshTokenJpaAdapter
    implements SaveRefreshTokenPort, LoadRefreshTokenPort, RevokeRefreshTokenPort {

  private final RefreshTokenJpaRepository repository;

  public RefreshTokenJpaAdapter(RefreshTokenJpaRepository repository) {
    this.repository = repository;
  }

  @Override
  public void save(RefreshToken token) {
    repository.save(toEntity(token));
  }

  @Override
  public Optional<RefreshToken> findByTokenHash(String tokenHash) {
    return repository.findByTokenHash(tokenHash).map(this::toDomain);
  }

  private RefreshToken toDomain(RefreshTokenEntity entity) {
    return RefreshToken.reconstitute(
        entity.id, new UserId(entity.userId), entity.tokenHash, entity.expiresAt, entity.revoked);
  }

  private RefreshTokenEntity toEntity(RefreshToken token) {
    var entity = new RefreshTokenEntity();
    entity.id = token.getId();
    entity.userId = token.getUserId().getValue();
    entity.tokenHash = token.getTokenHash();
    entity.expiresAt = token.getExpiresAt();
    entity.revoked = token.isRevoked();
    return entity;
  }
}
