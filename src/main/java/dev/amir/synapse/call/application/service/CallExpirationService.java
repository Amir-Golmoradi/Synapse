package dev.amir.synapse.call.application.service;

import dev.amir.synapse.call.application.port.out.CallReservationPort;
import dev.amir.synapse.call.application.port.out.CallRuntimePort;
import dev.amir.synapse.call.domain.port.out.LoadCallPort;
import dev.amir.synapse.call.domain.port.out.SaveCallPort;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CallExpirationService {
  private final LoadCallPort calls;
  private final SaveCallPort saveCalls;
  private final CallRuntimePort runtime;
  private final CallReservationPort reservations;
  private final CallNotifications notifications;

  public CallExpirationService(
      LoadCallPort calls,
      SaveCallPort saveCalls,
      CallRuntimePort runtime,
      CallReservationPort reservations,
      CallNotifications notifications) {
    this.calls = calls;
    this.saveCalls = saveCalls;
    this.runtime = runtime;
    this.reservations = reservations;
    this.notifications = notifications;
  }

  @Transactional
  public void expire(UUID callId, Instant now) {
    var call = calls.findByIdForUpdate(callId).orElse(null);
    if (call == null || call.status().isTerminal()) {
      return;
    }
    var state = runtime.findByCallId(callId).orElse(null);
    var shouldExpireDeadline = call.deadlineAt() != null && !now.isBefore(call.deadlineAt());
    if (shouldExpireDeadline) {
      call.expire(now);
    } else if (state != null && !now.isBefore(state.callerLeaseExpiresAt())) {
      call.expireUnreachable(call.participants().callerId(), now);
    } else if (state != null
        && state.calleeLeaseExpiresAt() != null
        && !now.isBefore(state.calleeLeaseExpiresAt())) {
      call.expireUnreachable(call.participants().calleeId(), now);
    } else {
      return;
    }
    var generation = state == null ? 0 : state.generation();
    var saved = saveCalls.saveAndFlush(call);
    runtime.delete(callId);
    reservations.release(callId);
    var type =
        switch (saved.status()) {
          case MISSED -> "CALL_MISSED";
          case CANCELLED -> "CALL_CANCELLED";
          case FAILED -> "CALL_FAILED";
          default -> "CALL_ENDED";
        };
    notifications.publish(type, saved, generation);
  }
}
