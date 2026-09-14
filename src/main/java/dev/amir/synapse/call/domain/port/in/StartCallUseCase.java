package dev.amir.synapse.call.domain.port.in;

import dev.amir.synapse.call.domain.enums.CallMediaType;
import java.util.UUID;

@FunctionalInterface
public interface StartCallUseCase {
  Result handle(Command command);

  record Command(
      UUID callerId,
      UUID calleeId,
      UUID clientRequestId,
      UUID clientInstanceId,
      CallMediaType mediaType) {}

  record Result(CallView call, boolean created) {}
}
