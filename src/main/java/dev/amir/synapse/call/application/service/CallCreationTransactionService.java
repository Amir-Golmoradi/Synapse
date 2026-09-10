package dev.amir.synapse.call.application.service;

import dev.amir.synapse.call.application.model.CallRuntimeState;
import dev.amir.synapse.call.application.model.CallSettings;
import dev.amir.synapse.call.application.port.out.CallReservationPort;
import dev.amir.synapse.call.application.port.out.CallRuntimePort;
import dev.amir.synapse.call.domain.model.Call;
import dev.amir.synapse.call.domain.port.out.SaveCallPort;
import dev.amir.synapse.call.domain.value_object.CallParticipants;
import java.time.Clock;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CallCreationTransactionService {
  private final SaveCallPort calls;
  private final CallRuntimePort runtime;
  private final CallReservationPort reservations;
  private final CallSettings properties;
  private final Clock clock;
  private final String serverInstanceId;

  public CallCreationTransactionService(
      SaveCallPort calls,
      CallRuntimePort runtime,
      CallReservationPort reservations,
      CallSettings properties,
      Clock callClock,
      @Qualifier("callServerInstanceId") String serverInstanceId) {
    this.calls = calls;
    this.runtime = runtime;
    this.reservations = reservations;
    this.properties = properties;
    this.clock = callClock;
    this.serverInstanceId = serverInstanceId;
  }

  @Transactional
  public Call create(
      UUID caller, UUID callee, UUID requestId, UUID instanceId, String fingerprint) {
    var now = clock.instant();
    var call =
        Call.start(
            new CallParticipants(caller, callee),
            requestId,
            fingerprint,
            now,
            now.plus(properties.ringingTimeout()));
    var saved = calls.saveAndFlush(call);
    reservations.reserve(saved.getId().value(), caller, callee);
    runtime.save(
        new CallRuntimeState(
            saved.getId().value(),
            instanceId,
            null,
            0,
            false,
            false,
            false,
            false,
            now.plus(properties.livenessLease()),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            0,
            0,
            serverInstanceId));
    return saved;
  }
}
