package dev.amir.synapse.call.domain.port.in;

import java.util.Optional;
import java.util.UUID;

public interface GetCurrentCallUseCase {
  Optional<CallView> getCurrent(UUID requesterId);
}
