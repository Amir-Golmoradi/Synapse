package dev.amir.synapse.call.application.service;

import dev.amir.synapse.call.application.model.CallRuntimeState;
import dev.amir.synapse.call.application.model.CallSettings;
import dev.amir.synapse.call.application.port.out.CallClientConnectionPort;
import dev.amir.synapse.call.application.port.out.CallReservationPort;
import dev.amir.synapse.call.application.port.out.CallRuntimePort;
import dev.amir.synapse.call.domain.enums.CallStatus;
import dev.amir.synapse.call.domain.exception.CallOperationException;
import dev.amir.synapse.call.domain.model.Call;
import dev.amir.synapse.call.domain.port.in.AcceptCallUseCase;
import dev.amir.synapse.call.domain.port.in.CallView;
import dev.amir.synapse.call.domain.port.in.EndCallUseCase;
import dev.amir.synapse.call.domain.port.in.GetCallUseCase;
import dev.amir.synapse.call.domain.port.in.GetCurrentCallUseCase;
import dev.amir.synapse.call.domain.port.in.RejectCallUseCase;
import dev.amir.synapse.call.domain.port.in.ResumeCallUseCase;
import dev.amir.synapse.call.domain.port.out.LoadCallPort;
import dev.amir.synapse.call.domain.port.out.SaveCallPort;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CallLifecycleService
    implements AcceptCallUseCase,
        RejectCallUseCase,
        EndCallUseCase,
        ResumeCallUseCase,
        GetCallUseCase,
        GetCurrentCallUseCase {
  private final LoadCallPort calls;
  private final SaveCallPort saveCalls;
  private final CallRuntimePort runtime;
  private final CallReservationPort reservations;
  private final CallClientConnectionPort clients;
  private final CallNotifications notifications;
  private final CallSettings properties;
  private final Clock clock;
  private final String serverInstanceId;

  public CallLifecycleService(
      LoadCallPort calls,
      SaveCallPort saveCalls,
      CallRuntimePort runtime,
      CallReservationPort reservations,
      CallClientConnectionPort clients,
      CallNotifications notifications,
      CallSettings properties,
      Clock callClock,
      @Qualifier("callServerInstanceId") String serverInstanceId) {
    this.calls = calls;
    this.saveCalls = saveCalls;
    this.runtime = runtime;
    this.reservations = reservations;
    this.clients = clients;
    this.notifications = notifications;
    this.properties = properties;
    this.clock = callClock;
    this.serverInstanceId = serverInstanceId;
  }

  @Override
  @Transactional
  public CallView accept(UUID callId, UUID actorId, UUID clientInstanceId) {
    requireIds(callId, actorId, clientInstanceId);
    var call = locked(callId, actorId);
    var state = requiredRuntime(callId);
    var now = clock.instant();
    var expired = expireIfDue(call, now, state.generation());
    if (expired.isPresent()) {
      return expired.orElseThrow();
    }
    if (state.calleeClientInstanceId() != null
        && !state.calleeClientInstanceId().equals(clientInstanceId)) {
      throw CallOperationException.wrongClient();
    }
    if (!clients.isConnected(actorId, clientInstanceId)) {
      throw CallOperationException.clientNotReady();
    }
    if (call.status() != CallStatus.RINGING) {
      return CallView.from(call, state.generation());
    }
    call.accept(actorId, now, now.plus(properties.connectionTimeout()));
    var saved = saveCalls.saveAndFlush(call);
    state = runtime.save(state.withCallee(clientInstanceId, now.plus(properties.livenessLease())));
    notifications.publish("CALL_ACCEPTED", saved, state.generation());
    return CallView.from(saved, state.generation());
  }

  @Override
  @Transactional
  public CallView reject(UUID callId, UUID actorId) {
    requireIds(callId, actorId);
    var call = locked(callId, actorId);
    var generation = runtime.findByCallId(callId).map(CallRuntimeState::generation).orElse(0);
    var now = clock.instant();
    var expired = expireIfDue(call, now, generation);
    if (expired.isPresent()) {
      return expired.orElseThrow();
    }
    if (call.status() == CallStatus.REJECTED) {
      return CallView.from(call, generation);
    }
    call.reject(actorId, now);
    var saved = saveCalls.saveAndFlush(call);
    complete(saved);
    notifications.publish("CALL_REJECTED", saved, generation);
    return CallView.from(saved, generation);
  }

  @Override
  @Transactional
  public CallView end(UUID callId, UUID actorId, EndReason reason) {
    requireIds(callId, actorId);
    if (reason == null) {
      throw new dev.amir.synapse.call.domain.exception.CallValidationException(
          "End reason is required.");
    }
    var call = locked(callId, actorId);
    var generation = runtime.findByCallId(callId).map(CallRuntimeState::generation).orElse(0);
    var now = clock.instant();
    var expired = expireIfDue(call, now, generation);
    if (expired.isPresent()) {
      return expired.orElseThrow();
    }
    if (call.status().isTerminal()) {
      return CallView.from(call, generation);
    }
    call.end(actorId, reason == EndReason.MEDIA_ERROR, now);
    var saved = saveCalls.saveAndFlush(call);
    if (saved.status().isTerminal()) {
      complete(saved);
    }
    var eventType =
        switch (saved.status()) {
          case CANCELLED -> "CALL_CANCELLED";
          case FAILED -> "CALL_FAILED";
          default -> "CALL_ENDED";
        };
    notifications.publish(eventType, saved, generation);
    return CallView.from(saved, generation);
  }

  @Override
  @Transactional
  public CallView resume(ResumeCallUseCase.Command command) {
    if (command == null) {
      throw new dev.amir.synapse.call.domain.exception.CallValidationException(
          "Resume request is required.");
    }
    requireIds(
        command.callId(), command.actorId(), command.clientInstanceId(), command.requestId());
    var call = locked(command.callId(), command.actorId());
    if (call.status().isTerminal()) {
      throw CallOperationException.conflict();
    }
    var state = requiredRuntime(command.callId());
    var now = clock.instant();
    var expired = expireIfDue(call, now, state.generation());
    if (expired.isPresent()) {
      return expired.orElseThrow();
    }
    requireOwnerAndConnection(call, state, command.actorId(), command.clientInstanceId());
    var previousRequest =
        command.actorId().equals(call.participants().callerId())
            ? state.callerResumeRequestId()
            : state.calleeResumeRequestId();
    if (command.requestId().equals(previousRequest)) {
      return CallView.from(call, state.generation());
    }
    var canRetain =
        command.peerConnectionRetained()
            && command.observedGeneration() == state.generation()
            && serverInstanceId.equals(state.serverInstanceId());
    state =
        state.renew(
            command.actorId(),
            call.participants().callerId(),
            now.plus(properties.livenessLease()));
    if (call.status() == CallStatus.RINGING) {
      if (!command.actorId().equals(call.participants().callerId())) {
        throw CallOperationException.forbidden();
      }
      state =
          state.recordResume(
              command.actorId(), call.participants().callerId(), command.requestId());
      runtime.save(state);
      return CallView.from(call, state.generation());
    }
    if (!canRetain) {
      if (call.status() == CallStatus.ACTIVE) {
        call.startRecovery(now, now.plus(properties.recoveryTimeout()));
        call = saveCalls.saveAndFlush(call);
      }
      state =
          state.nextGeneration(
              command.actorId(),
              call.participants().callerId(),
              command.requestId(),
              serverInstanceId);
      notifications.publish("CALL_RECOVERING", call, state.generation());
    } else {
      state =
          state.recordResume(
              command.actorId(), call.participants().callerId(), command.requestId());
    }
    runtime.save(state);
    return CallView.from(call, state.generation());
  }

  @Override
  @Transactional(readOnly = true)
  public CallView get(UUID callId, UUID requesterId) {
    return view(
        requireAuthorized(
            calls.findById(callId).orElseThrow(CallOperationException::notFound), requesterId));
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<CallView> getCurrent(UUID requesterId) {
    if (requesterId == null) {
      throw CallOperationException.notFound();
    }
    return calls
        .findCurrentByParticipant(requesterId)
        .map(call -> view(requireAuthorized(call, requesterId)));
  }

  private CallView view(Call call) {
    var generation =
        runtime.findByCallId(call.getId().value()).map(CallRuntimeState::generation).orElse(0);
    return CallView.from(call, generation);
  }

  private Call locked(UUID callId, UUID actor) {
    return requireAuthorized(
        calls.findByIdForUpdate(callId).orElseThrow(CallOperationException::notFound), actor);
  }

  private static Call requireAuthorized(Call call, UUID actor) {
    if (actor == null || !call.participants().contains(actor)) {
      throw CallOperationException.notFound();
    }
    return call;
  }

  private CallRuntimeState requiredRuntime(UUID callId) {
    return runtime.findByCallId(callId).orElseThrow(CallOperationException::conflict);
  }

  private void requireOwnerAndConnection(
      Call call, CallRuntimeState state, UUID actor, UUID instance) {
    var owner =
        actor.equals(call.participants().callerId())
            ? state.callerClientInstanceId()
            : state.calleeClientInstanceId();
    if (!instance.equals(owner)) {
      throw CallOperationException.wrongClient();
    }
    if (!clients.isConnected(actor, instance)) {
      throw CallOperationException.clientNotReady();
    }
  }

  private void complete(Call call) {
    runtime.delete(call.getId().value());
    reservations.release(call.getId().value());
  }

  private Optional<CallView> expireIfDue(Call call, java.time.Instant now, int generation) {
    if (call.deadlineAt() == null || now.isBefore(call.deadlineAt())) {
      return Optional.empty();
    }
    call.expire(now);
    var saved = saveCalls.saveAndFlush(call);
    complete(saved);
    var type = saved.status() == CallStatus.MISSED ? "CALL_MISSED" : "CALL_FAILED";
    notifications.publish(type, saved, generation);
    return Optional.of(CallView.from(saved, generation));
  }

  private static void requireIds(Object... ids) {
    for (var id : ids) {
      if (id == null) {
        throw new dev.amir.synapse.call.domain.exception.CallValidationException(
            "Call identifiers are required.");
      }
    }
  }
}
