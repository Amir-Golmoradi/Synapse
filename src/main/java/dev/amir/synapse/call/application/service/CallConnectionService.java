package dev.amir.synapse.call.application.service;

import dev.amir.synapse.call.application.model.CallNegotiationInstruction;
import dev.amir.synapse.call.application.model.CallRuntimeState;
import dev.amir.synapse.call.application.model.CallSettings;
import dev.amir.synapse.call.application.port.out.CallClientConnectionPort;
import dev.amir.synapse.call.application.port.out.CallRuntimePort;
import dev.amir.synapse.call.application.port.out.CallSignalDeliveryPort;
import dev.amir.synapse.call.domain.enums.CallStatus;
import dev.amir.synapse.call.domain.exception.CallOperationException;
import dev.amir.synapse.call.domain.model.Call;
import dev.amir.synapse.call.domain.port.out.LoadCallPort;
import dev.amir.synapse.call.domain.port.out.SaveCallPort;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CallConnectionService {
  private final LoadCallPort calls;
  private final SaveCallPort saveCalls;
  private final CallRuntimePort runtime;
  private final CallClientConnectionPort clients;
  private final CallSignalDeliveryPort delivery;
  private final CallNotifications notifications;
  private final CallSettings properties;
  private final Clock clock;

  public CallConnectionService(
      LoadCallPort calls,
      SaveCallPort saveCalls,
      CallRuntimePort runtime,
      CallClientConnectionPort clients,
      CallSignalDeliveryPort delivery,
      CallNotifications notifications,
      CallSettings properties,
      Clock callClock) {
    this.calls = calls;
    this.saveCalls = saveCalls;
    this.runtime = runtime;
    this.clients = clients;
    this.delivery = delivery;
    this.notifications = notifications;
    this.properties = properties;
    this.clock = callClock;
  }

  public void register(UUID userId, UUID clientInstanceId, String sessionId) {
    if (userId == null || clientInstanceId == null || sessionId == null) {
      throw new dev.amir.synapse.call.domain.exception.CallValidationException(
          "Client registration is invalid.");
    }
    clients.register(userId, clientInstanceId, sessionId);
  }

  public void disconnect(String sessionId) {
    if (sessionId != null) {
      clients.disconnect(sessionId);
    }
  }

  @Transactional
  public void ready(
      UUID callId, UUID actor, UUID clientInstanceId, int generation, String sessionId) {
    var context = context(callId, actor, clientInstanceId, generation, sessionId);
    var now = clock.instant();
    var negotiationAlreadyReady = context.state().bothReady();
    var state =
        context
            .state()
            .ready(actor, context.call().participants().callerId())
            .renew(
                actor,
                context.call().participants().callerId(),
                now.plus(properties.livenessLease()));
    state = runtime.save(state);
    if (!negotiationAlreadyReady && state.bothReady()) {
      var caller = context.call().participants().callerId();
      var callerSession = ownerSession(context.call(), state, caller);
      delivery.sendInstruction(
          caller,
          callerSession,
          new CallNegotiationInstruction("NEGOTIATION_READY", callId, generation, true));
    }
  }

  @Transactional
  public void connected(
      UUID callId, UUID actor, UUID clientInstanceId, int generation, String sessionId) {
    var context = context(callId, actor, clientInstanceId, generation, sessionId);
    var now = clock.instant();
    var state =
        context
            .state()
            .connected(actor, context.call().participants().callerId())
            .renew(
                actor,
                context.call().participants().callerId(),
                now.plus(properties.livenessLease()));
    state = runtime.save(state);
    if (state.bothConnected() && context.call().status() != CallStatus.ACTIVE) {
      context.call().markConnected(now);
      var saved = saveCalls.saveAndFlush(context.call());
      notifications.publish("CALL_ACTIVE", saved, generation);
    }
  }

  @Transactional
  public void heartbeat(
      UUID callId, UUID actor, UUID clientInstanceId, int generation, String sessionId) {
    var context = heartbeatContext(callId, actor, clientInstanceId, generation, sessionId);
    runtime.save(
        context
            .state()
            .renew(
                actor,
                context.call().participants().callerId(),
                clock.instant().plus(properties.livenessLease())));
  }

  private Context heartbeatContext(
      UUID callId, UUID actor, UUID instanceId, int generation, String sessionId) {
    var call = calls.findByIdForUpdate(callId).orElseThrow(CallOperationException::notFound);
    if (!call.participants().contains(actor)) {
      throw CallOperationException.notFound();
    }
    if (call.status().isTerminal()) {
      throw CallOperationException.conflict();
    }
    if (call.status() == CallStatus.RINGING && !actor.equals(call.participants().callerId())) {
      throw CallOperationException.forbidden();
    }
    var state = runtime.findByCallId(callId).orElseThrow(CallOperationException::conflict);
    if (state.generation() != generation) {
      throw CallOperationException.staleGeneration();
    }
    var owner =
        actor.equals(call.participants().callerId())
            ? state.callerClientInstanceId()
            : state.calleeClientInstanceId();
    if (!instanceId.equals(owner)
        || clients.sessionId(actor, instanceId).filter(sessionId::equals).isEmpty()) {
      throw CallOperationException.wrongClient();
    }
    return new Context(call, state);
  }

  private Context context(
      UUID callId, UUID actor, UUID instanceId, int generation, String sessionId) {
    var call = calls.findByIdForUpdate(callId).orElseThrow(CallOperationException::notFound);
    if (!call.participants().contains(actor)) {
      throw CallOperationException.notFound();
    }
    if (call.status().isTerminal() || call.status() == CallStatus.RINGING) {
      throw CallOperationException.conflict();
    }
    var state = runtime.findByCallId(callId).orElseThrow(CallOperationException::conflict);
    if (state.generation() != generation) {
      throw CallOperationException.staleGeneration();
    }
    var owner =
        actor.equals(call.participants().callerId())
            ? state.callerClientInstanceId()
            : state.calleeClientInstanceId();
    if (!instanceId.equals(owner)
        || !clients.sessionId(actor, instanceId).filter(sessionId::equals).isPresent()) {
      throw CallOperationException.wrongClient();
    }
    return new Context(call, state);
  }

  private String ownerSession(Call call, CallRuntimeState state, UUID userId) {
    var instance =
        userId.equals(call.participants().callerId())
            ? state.callerClientInstanceId()
            : state.calleeClientInstanceId();
    if (instance == null) {
      throw CallOperationException.clientNotReady();
    }
    return clients.sessionId(userId, instance).orElseThrow(CallOperationException::clientNotReady);
  }

  private record Context(Call call, CallRuntimeState state) {}
}
