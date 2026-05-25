package dev.amir.synapse.identity.domain.port.in.get_current_user;

import dev.amir.synapse.identity.domain.value_object.UserId;

public record GetCurrentUserQuery(UserId userId) {}
