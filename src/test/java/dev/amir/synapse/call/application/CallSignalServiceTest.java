package dev.amir.synapse.call.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.amir.synapse.call.application.model.CallRuntimeState;
import dev.amir.synapse.call.application.model.CallSignal;
import dev.amir.synapse.call.application.port.out.CallClientConnectionPort;
import dev.amir.synapse.call.application.port.out.CallRuntimePort;
import dev.amir.synapse.call.application.port.out.CallSignalDeliveryPort;
import dev.amir.synapse.call.application.service.CallSignalService;
import dev.amir.synapse.call.domain.exception.CallOperationException;
import dev.amir.synapse.call.domain.model.Call;
import dev.amir.synapse.call.domain.port.out.LoadCallPort;
import dev.amir.synapse.call.domain.value_object.CallParticipants;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CallSignalServiceTest {
  private final UUID caller = UUID.randomUUID();
  private final UUID callee = UUID.randomUUID();
  private final UUID callerInstance = UUID.randomUUID();
  private final UUID calleeInstance = UUID.randomUUID();
  private final UUID callId = UUID.randomUUID();
  private final RuntimeStub runtime = new RuntimeStub();
  private final DeliveryStub delivery = new DeliveryStub();
  private CallSignalService service;

  @BeforeEach
  void setUp() {
    var now = Instant.parse("2026-09-10T10:00:00Z");
    var call =
        Call.rehydrate(
            new dev.amir.synapse.call.domain.model.CallSnapshot(
                dev.amir.synapse.call.domain.value_object.CallId.of(callId),
                new CallParticipants(caller, callee),
                UUID.randomUUID(),
                "fingerprint",
                dev.amir.synapse.call.domain.enums.CallStatus.CONNECTING,
                now,
                now,
                now,
                null,
                null,
                now.plusSeconds(30),
                null,
                null,
                0L));
    runtime.state = runtimeState();
    service = new CallSignalService(new CallsStub(call), runtime, new ConnectionsStub(), delivery);
  }

  @Test
  void persistsOfferMetadataAndSuppressesAnExactRetry() {
    var signal =
        new CallSignal(
            UUID.randomUUID(), 1, CallSignal.Type.OFFER, "test-sdp", null, null, null, null, null);

    service.relay(callId, caller, callerInstance, "caller-session", signal);
    service.relay(callId, caller, callerInstance, "caller-session", signal);

    assertThat(runtime.saveCount).isEqualTo(1);
    assertThat(runtime.state.offerMessageId()).isEqualTo(signal.messageId());
    assertThat(delivery.signalCount).isEqualTo(1);
  }

  @Test
  void rejectsASecondOfferForTheSameGeneration() {
    var first =
        new CallSignal(
            UUID.randomUUID(), 1, CallSignal.Type.OFFER, "first", null, null, null, null, null);
    var second =
        new CallSignal(
            UUID.randomUUID(), 1, CallSignal.Type.OFFER, "second", null, null, null, null, null);
    service.relay(callId, caller, callerInstance, "caller-session", first);

    assertThatThrownBy(
            () -> service.relay(callId, caller, callerInstance, "caller-session", second))
        .isInstanceOf(CallOperationException.class);
  }

  private CallRuntimeState runtimeState() {
    var lease = Instant.parse("2026-09-10T10:01:00Z");
    return new CallRuntimeState(
        callId,
        callerInstance,
        calleeInstance,
        1,
        true,
        true,
        false,
        false,
        lease,
        lease,
        null,
        null,
        null,
        null,
        null,
        null,
        0,
        0,
        "test-server");
  }

  private static final class CallsStub implements LoadCallPort {
    private final Call call;

    private CallsStub(Call call) {
      this.call = call;
    }

    @Override
    public Optional<Call> findById(UUID id) {
      return Optional.of(call);
    }

    @Override
    public Optional<Call> findByIdForUpdate(UUID id) {
      return Optional.of(call);
    }

    @Override
    public Optional<Call> findByCallerAndRequestId(UUID callerId, UUID requestId) {
      return Optional.empty();
    }

    @Override
    public Optional<Call> findCurrentByParticipant(UUID userId) {
      return Optional.empty();
    }
  }

  private final class ConnectionsStub implements CallClientConnectionPort {
    @Override
    public void register(UUID userId, UUID clientInstanceId, String sessionId) {}

    @Override
    public void disconnect(String sessionId) {}

    @Override
    public boolean isConnected(UUID userId, UUID clientInstanceId) {
      return true;
    }

    @Override
    public Optional<String> sessionId(UUID userId, UUID clientInstanceId) {
      if (userId.equals(caller) && clientInstanceId.equals(callerInstance)) {
        return Optional.of("caller-session");
      }
      if (userId.equals(callee) && clientInstanceId.equals(calleeInstance)) {
        return Optional.of("callee-session");
      }
      return Optional.empty();
    }
  }

  private static final class RuntimeStub implements CallRuntimePort {
    private CallRuntimeState state;
    private int saveCount;

    @Override
    public Optional<CallRuntimeState> findByCallId(UUID id) {
      return Optional.of(state);
    }

    @Override
    public CallRuntimeState save(CallRuntimeState next) {
      state = next;
      saveCount++;
      return next;
    }

    @Override
    public void delete(UUID id) {}
  }

  private static final class DeliveryStub implements CallSignalDeliveryPort {
    private int signalCount;

    @Override
    public void sendSignal(
        UUID userId, String sessionId, UUID id, UUID senderId, CallSignal signal) {
      signalCount++;
    }

    @Override
    public void sendInstruction(UUID userId, String sessionId, Object instruction) {}
  }
}
