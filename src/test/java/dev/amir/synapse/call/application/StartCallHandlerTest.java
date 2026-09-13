package dev.amir.synapse.call.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.amir.synapse.call.application.command.StartCallHandler;
import dev.amir.synapse.call.application.port.out.CallClientConnectionPort;
import dev.amir.synapse.call.application.port.out.CallRuntimePort;
import dev.amir.synapse.call.application.service.CallCreationTransactionService;
import dev.amir.synapse.call.application.service.CallNotifications;
import dev.amir.synapse.call.domain.enums.CallMediaType;
import dev.amir.synapse.call.domain.exception.CallOperationException;
import dev.amir.synapse.call.domain.model.Call;
import dev.amir.synapse.call.domain.port.in.StartCallUseCase;
import dev.amir.synapse.call.domain.port.out.LoadCallPort;
import dev.amir.synapse.call.domain.value_object.CallParticipants;
import dev.amir.synapse.identity.application.api.user_lookup.UserLookupUseCase;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StartCallHandlerTest {
  private final LoadCallPort calls = mock(LoadCallPort.class);
  private final CallRuntimePort runtime = mock(CallRuntimePort.class);
  private final CallClientConnectionPort clients = mock(CallClientConnectionPort.class);
  private final UserLookupUseCase users = mock(UserLookupUseCase.class);
  private final CallCreationTransactionService creation =
      mock(CallCreationTransactionService.class);
  private final CallNotifications notifications = mock(CallNotifications.class);
  private StartCallHandler handler;

  @BeforeEach
  void setUp() {
    handler = new StartCallHandler(calls, runtime, clients, users, creation, notifications);
  }

  @Test
  void createsAVideoCallAndPassesMediaTypeToTheTransaction() {
    var caller = UUID.randomUUID();
    var callee = UUID.randomUUID();
    var requestId = UUID.randomUUID();
    var instanceId = UUID.randomUUID();
    var command =
        new StartCallUseCase.Command(caller, callee, requestId, instanceId, CallMediaType.VIDEO);
    var created = call(caller, callee, CallMediaType.VIDEO, requestId, "stored-fingerprint");
    when(calls.findByCallerAndRequestId(caller, requestId)).thenReturn(Optional.empty());
    when(clients.isConnected(caller, instanceId)).thenReturn(true);
    when(users.existsByUserId(caller)).thenReturn(true);
    when(users.existsByUserId(callee)).thenReturn(true);
    when(creation.create(
            any(UUID.class),
            any(UUID.class),
            any(UUID.class),
            any(UUID.class),
            any(CallMediaType.class),
            anyString()))
        .thenReturn(created);

    var result = handler.handle(command);

    assertThat(result.created()).isTrue();
    assertThat(result.call().mediaType()).isEqualTo(CallMediaType.VIDEO);
    verify(creation)
        .create(
            eq(caller),
            eq(callee),
            eq(requestId),
            eq(instanceId),
            eq(CallMediaType.VIDEO),
            anyString());
    verify(notifications).publish("CALL_INCOMING", created, 0);
  }

  @Test
  void rejectsReuseOfARequestForAnotherMediaType() {
    var caller = UUID.randomUUID();
    var callee = UUID.randomUUID();
    var requestId = UUID.randomUUID();
    var instanceId = UUID.randomUUID();
    var existing = call(caller, callee, CallMediaType.VOICE, requestId, "voice-fingerprint");
    when(calls.findByCallerAndRequestId(caller, requestId)).thenReturn(Optional.of(existing));

    assertThatThrownBy(
            () ->
                handler.handle(
                    new StartCallUseCase.Command(
                        caller, callee, requestId, instanceId, CallMediaType.VIDEO)))
        .isInstanceOf(CallOperationException.class)
        .extracting(exception -> ((CallOperationException) exception).getErrorCode())
        .isEqualTo("CALL_IDEMPOTENCY_CONFLICT");
  }

  private static Call call(
      UUID caller, UUID callee, CallMediaType mediaType, UUID requestId, String fingerprint) {
    var now = Instant.parse("2026-09-13T08:00:00Z");
    return Call.start(
        new CallParticipants(caller, callee),
        mediaType,
        requestId,
        fingerprint,
        now,
        now.plusSeconds(45));
  }
}
