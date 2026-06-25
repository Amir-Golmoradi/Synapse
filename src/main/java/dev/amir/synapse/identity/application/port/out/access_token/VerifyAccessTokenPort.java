package dev.amir.synapse.identity.application.port.out.access_token;

import dev.amir.synapse.identity.domain.value_object.UserId;
import java.util.Optional;

@FunctionalInterface
public interface VerifyAccessTokenPort {
  Optional<UserId> verify(String accessToken);
}
