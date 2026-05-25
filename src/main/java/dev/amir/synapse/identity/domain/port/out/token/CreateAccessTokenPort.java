package dev.amir.synapse.identity.domain.port.out.token;

import dev.amir.synapse.identity.domain.value_object.UserId;

@FunctionalInterface
public interface CreateAccessTokenPort {

  String createAccessToken(UserId userId);
}
