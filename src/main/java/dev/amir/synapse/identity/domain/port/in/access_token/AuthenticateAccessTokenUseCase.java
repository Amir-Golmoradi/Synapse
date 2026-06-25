package dev.amir.synapse.identity.domain.port.in.access_token;

import dev.amir.synapse.identity.domain.value_object.UserId;
import java.util.Optional;

@FunctionalInterface
public interface AuthenticateAccessTokenUseCase {
  Optional<UserId> handle(AuthenticateAccessTokenQuery query);
}
