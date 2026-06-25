package dev.amir.synapse.identity.application.port.out.access_token;

import dev.amir.synapse.identity.domain.value_object.UserId;

@FunctionalInterface
public interface CreateAccessTokenPort {

  String createAccessToken(UserId userId);
}
