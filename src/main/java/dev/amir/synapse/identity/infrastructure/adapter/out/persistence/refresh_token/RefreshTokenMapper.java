package dev.amir.synapse.identity.infrastructure.adapter.out.persistence.refresh_token;

import dev.amir.synapse.identity.domain.entity.RefreshToken;
import dev.amir.synapse.identity.domain.value_object.UserId;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenMapper {

  public RefreshToken toDomain(RefreshTokenEntity entity) {
    return RefreshToken.reconstitute(
        entity.getId(),
        UserId.of(entity.getUserId().toString()),
        entity.getTokenHash(),
        entity.getExpiresAt(),
        entity.isRevoked());
  }

  public RefreshTokenEntity toEntity(RefreshToken token) {
    return RefreshTokenEntity.reconstitute(
        token.getId(),
        token.getUserId(),
        token.getTokenHash(),
        token.getExpiresAt(),
        token.isRevoked());
  }
}
