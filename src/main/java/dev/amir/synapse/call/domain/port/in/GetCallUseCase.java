package dev.amir.synapse.call.domain.port.in;

import java.util.UUID;

public interface GetCallUseCase {
  CallView get(UUID callId, UUID requesterId);
}
