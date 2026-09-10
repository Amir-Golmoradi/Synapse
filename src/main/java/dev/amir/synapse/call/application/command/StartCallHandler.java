package dev.amir.synapse.call.application.command;

import dev.amir.synapse.call.application.model.CallRuntimeState;
import dev.amir.synapse.call.application.port.out.CallClientConnectionPort;
import dev.amir.synapse.call.application.port.out.CallRuntimePort;
import dev.amir.synapse.call.application.service.CallCreationTransactionService;
import dev.amir.synapse.call.application.service.CallNotifications;
import dev.amir.synapse.call.domain.exception.CallOperationException;
import dev.amir.synapse.call.domain.exception.CallValidationException;
import dev.amir.synapse.call.domain.port.in.CallView;
import dev.amir.synapse.call.domain.port.in.StartCallUseCase;
import dev.amir.synapse.call.domain.port.out.LoadCallPort;
import dev.amir.synapse.identity.application.api.user_lookup.UserLookupUseCase;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class StartCallHandler implements StartCallUseCase {
  private final LoadCallPort calls;
  private final CallRuntimePort runtime;
  private final CallClientConnectionPort clients;
  private final UserLookupUseCase users;
  private final CallCreationTransactionService creation;
  private final CallNotifications notifications;

  public StartCallHandler(
      LoadCallPort calls,
      CallRuntimePort runtime,
      CallClientConnectionPort clients,
      UserLookupUseCase users,
      CallCreationTransactionService creation,
      CallNotifications notifications) {
    this.calls = calls;
    this.runtime = runtime;
    this.clients = clients;
    this.users = users;
    this.creation = creation;
    this.notifications = notifications;
  }

  @Override
  public Result handle(Command command) {
    require(command);
    var fingerprint = fingerprint(command.calleeId(), command.clientInstanceId());
    var existing = calls.findByCallerAndRequestId(command.callerId(), command.clientRequestId());
    if (existing.isPresent()) {
      var call = existing.orElseThrow();
      if (!call.startRequestFingerprint().equals(fingerprint)) {
        throw CallOperationException.idempotencyConflict();
      }
      var generation =
          runtime.findByCallId(call.getId().value()).map(CallRuntimeState::generation).orElse(0);
      return new Result(CallView.from(call, generation), false);
    }
    if (!clients.isConnected(command.callerId(), command.clientInstanceId())) {
      throw CallOperationException.clientNotReady();
    }
    if (!users.existsByUserId(command.callerId()) || !users.existsByUserId(command.calleeId())) {
      throw CallOperationException.notFound();
    }
    dev.amir.synapse.call.domain.model.Call created;
    try {
      created =
          creation.create(
              command.callerId(),
              command.calleeId(),
              command.clientRequestId(),
              command.clientInstanceId(),
              fingerprint);
    } catch (CallOperationException exception) {
      if (!"CALL_BUSY".equals(exception.getErrorCode())) {
        throw exception;
      }
      return concurrentResult(command, fingerprint).orElseThrow(() -> exception);
    } catch (DataIntegrityViolationException exception) {
      return concurrentResult(command, fingerprint)
          .orElseThrow(CallOperationException::idempotencyConflict);
    }
    notifications.publish("CALL_INCOMING", created, 0);
    return new Result(CallView.from(created, 0), true);
  }

  private Optional<Result> concurrentResult(Command command, String fingerprint) {
    return calls
        .findByCallerAndRequestId(command.callerId(), command.clientRequestId())
        .filter(call -> call.startRequestFingerprint().equals(fingerprint))
        .map(
            call -> {
              var generation =
                  runtime
                      .findByCallId(call.getId().value())
                      .map(CallRuntimeState::generation)
                      .orElse(0);
              return new Result(CallView.from(call, generation), false);
            });
  }

  private static void require(Command command) {
    if (command == null
        || command.callerId() == null
        || command.calleeId() == null
        || command.clientRequestId() == null
        || command.clientInstanceId() == null) {
      throw new CallValidationException("Call identifiers are required.");
    }
  }

  private static String fingerprint(Object... values) {
    try {
      var digest = MessageDigest.getInstance("SHA-256");
      for (var value : values) {
        digest.update(value.toString().getBytes(StandardCharsets.UTF_8));
        digest.update((byte) 0);
      }
      return HexFormat.of().formatHex(digest.digest());
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
  }
}
