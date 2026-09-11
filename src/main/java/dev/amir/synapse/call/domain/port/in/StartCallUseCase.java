package dev.amir.synapse.call.domain.port.in;

import java.util.UUID;

public interface StartCallUseCase {
  Result handle(Command command);

  record Command(UUID callerId, UUID calleeId, UUID clientRequestId, UUID clientInstanceId) {}

  record Result(CallView call, boolean created) {}
}
