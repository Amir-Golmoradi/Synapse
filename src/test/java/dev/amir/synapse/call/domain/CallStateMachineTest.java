package dev.amir.synapse.call.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.amir.synapse.call.domain.enums.CallStatus;
import dev.amir.synapse.call.domain.enums.CallTerminationReason;
import dev.amir.synapse.call.domain.exception.CallOperationException;
import dev.amir.synapse.call.domain.model.Call;
import dev.amir.synapse.call.domain.value_object.CallParticipants;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CallStateMachineTest {
  private final UUID caller = UUID.randomUUID();
  private final UUID callee = UUID.randomUUID();
  private final Instant now = Instant.parse("2026-09-10T10:00:00Z");

  @Test
  void acceptedCallConnectsRecoversAndEnds() {
    var call = call();

    call.accept(callee, now.plusSeconds(1), now.plusSeconds(31));
    call.markConnected(now.plusSeconds(2));
    call.startRecovery(now.plusSeconds(3), now.plusSeconds(33));
    call.markConnected(now.plusSeconds(4));
    call.end(caller, false, now.plusSeconds(5));

    assertThat(call.status()).isEqualTo(CallStatus.ENDED);
    assertThat(call.snapshot().acceptedAt()).isEqualTo(now.plusSeconds(1));
    assertThat(call.snapshot().connectedAt()).isEqualTo(now.plusSeconds(2));
    assertThat(call.snapshot().terminationReason()).isEqualTo(CallTerminationReason.HANGUP);
    assertThat(call.snapshot().terminatedBy()).isEqualTo(caller);
  }

  @Test
  void calleeMayRejectButCallerMayNot() {
    var call = call();

    assertThatThrownBy(() -> call.reject(caller, now.plusSeconds(1)))
        .isInstanceOf(CallOperationException.class)
        .extracting(exception -> ((CallOperationException) exception).getErrorCode())
        .isEqualTo("CALL_ACTION_FORBIDDEN");

    call.reject(callee, now.plusSeconds(1));
    assertThat(call.status()).isEqualTo(CallStatus.REJECTED);
  }

  @Test
  void ringingDeadlineProducesMissedCall() {
    var call = call();

    call.expire(now.plus(45, ChronoUnit.SECONDS));

    assertThat(call.status()).isEqualTo(CallStatus.MISSED);
    assertThat(call.snapshot().terminationReason()).isEqualTo(CallTerminationReason.NO_ANSWER);
  }

  @Test
  void terminalCallsRemainUnchangedOnDuplicateEnd() {
    var call = call();
    call.end(caller, false, now.plusSeconds(1));
    var terminal = call.snapshot();

    call.end(callee, true, now.plusSeconds(2));

    assertThat(call.snapshot()).isEqualTo(terminal);
  }

  @Test
  void acceptAfterDeadlineAndRejectAfterAcceptAreRejected() {
    var expiredInvitation = call();
    assertThatThrownBy(
            () -> expiredInvitation.accept(callee, now.plusSeconds(45), now.plusSeconds(75)))
        .isInstanceOf(CallOperationException.class);

    var accepted = call();
    accepted.accept(callee, now.plusSeconds(1), now.plusSeconds(31));
    assertThatThrownBy(() -> accepted.reject(callee, now.plusSeconds(2)))
        .isInstanceOf(CallOperationException.class);
  }

  private Call call() {
    return Call.start(
        new CallParticipants(caller, callee),
        UUID.randomUUID(),
        "fingerprint",
        now,
        now.plusSeconds(45));
  }
}
