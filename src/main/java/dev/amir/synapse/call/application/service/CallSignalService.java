package dev.amir.synapse.call.application.service;

import dev.amir.synapse.call.application.model.CallSignal;
import dev.amir.synapse.call.application.port.out.CallClientConnectionPort;
import dev.amir.synapse.call.application.port.out.CallRuntimePort;
import dev.amir.synapse.call.application.port.out.CallSignalDeliveryPort;
import dev.amir.synapse.call.domain.enums.CallStatus;
import dev.amir.synapse.call.domain.exception.CallOperationException;
import dev.amir.synapse.call.domain.exception.CallValidationException;
import dev.amir.synapse.call.domain.port.out.LoadCallPort;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CallSignalService {
  private static final int MAX_SDP_LENGTH = 32 * 1024;
  private static final int MAX_CANDIDATE_LENGTH = 2 * 1024;
  private static final int MAX_CANDIDATES_PER_PARTICIPANT = 256;
  private final LoadCallPort calls;
  private final CallRuntimePort runtime;
  private final CallClientConnectionPort clients;
  private final CallSignalDeliveryPort delivery;

  public CallSignalService(
      LoadCallPort calls,
      CallRuntimePort runtime,
      CallClientConnectionPort clients,
      CallSignalDeliveryPort delivery) {
    this.calls = calls;
    this.runtime = runtime;
    this.clients = clients;
    this.delivery = delivery;
  }

  @Transactional
  public void relay(
      UUID callId, UUID actor, UUID clientInstanceId, String sessionId, CallSignal signal) {
    validateSignal(signal);
    var call = calls.findByIdForUpdate(callId).orElseThrow(CallOperationException::notFound);
    if (!call.participants().contains(actor)) {
      throw CallOperationException.notFound();
    }
    if (call.status() != CallStatus.CONNECTING
        && call.status() != CallStatus.ACTIVE
        && call.status() != CallStatus.RECOVERING) {
      throw CallOperationException.conflict();
    }
    var state = runtime.findByCallId(callId).orElseThrow(CallOperationException::conflict);
    if (signal.generation() != state.generation()) {
      throw CallOperationException.staleGeneration();
    }
    var caller = call.participants().callerId();
    var isCaller = actor.equals(caller);
    var owner = isCaller ? state.callerClientInstanceId() : state.calleeClientInstanceId();
    if (!clientInstanceId.equals(owner)
        || clients.sessionId(actor, owner).filter(sessionId::equals).isEmpty()) {
      throw CallOperationException.wrongClient();
    }
    if ((signal.type() == CallSignal.Type.OFFER && !isCaller)
        || (signal.type() == CallSignal.Type.ANSWER && isCaller)) {
      throw CallOperationException.forbidden();
    }
    if (!state.bothReady()) {
      throw CallOperationException.clientNotReady();
    }
    var checked = recordSignal(state, signal, isCaller);
    if (checked.duplicate()) {
      return;
    }
    runtime.save(checked.state());
    var recipient = call.participants().counterpartOf(actor);
    var recipientInstance =
        isCaller ? state.calleeClientInstanceId() : state.callerClientInstanceId();
    if (recipientInstance == null) {
      throw CallOperationException.clientNotReady();
    }
    var recipientSession =
        clients
            .sessionId(recipient, recipientInstance)
            .orElseThrow(CallOperationException::clientNotReady);
    delivery.sendSignal(recipient, recipientSession, callId, actor, signal);
  }

  private static CheckedSignal recordSignal(
      dev.amir.synapse.call.application.model.CallRuntimeState state,
      CallSignal signal,
      boolean caller) {
    return switch (signal.type()) {
      case OFFER -> {
        var digest = digest(signal.sdp());
        if (state.offerMessageId() != null) {
          if (state.offerMessageId().equals(signal.messageId())
              && digest.equals(state.offerDigest())) {
            yield new CheckedSignal(state, true);
          }
          throw CallOperationException.conflict();
        }
        yield new CheckedSignal(state.recordOffer(signal.messageId(), digest), false);
      }
      case ANSWER -> {
        if (state.offerMessageId() == null
            || !state.offerMessageId().equals(signal.descriptionId())) {
          throw CallOperationException.conflict();
        }
        var digest = digest(signal.sdp());
        if (state.answerMessageId() != null) {
          if (state.answerMessageId().equals(signal.messageId())
              && digest.equals(state.answerDigest())) {
            yield new CheckedSignal(state, true);
          }
          throw CallOperationException.conflict();
        }
        yield new CheckedSignal(state.recordAnswer(signal.messageId(), digest), false);
      }
      case ICE_CANDIDATE -> {
        var expectedDescription = caller ? state.offerMessageId() : state.answerMessageId();
        var count = caller ? state.callerCandidateCount() : state.calleeCandidateCount();
        if (expectedDescription == null || !expectedDescription.equals(signal.descriptionId())) {
          throw CallOperationException.conflict();
        }
        if (count >= MAX_CANDIDATES_PER_PARTICIPANT) {
          throw new CallValidationException("Too many ICE candidates.");
        }
        yield new CheckedSignal(state.recordCandidate(caller), false);
      }
    };
  }

  private static String digest(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
  }

  private static void validateSignal(CallSignal signal) {
    if (signal == null
        || signal.messageId() == null
        || signal.type() == null
        || signal.generation() < 1) {
      throw new CallValidationException("Signal envelope is invalid.");
    }
    switch (signal.type()) {
      case OFFER -> requireSdp(signal, false);
      case ANSWER -> requireSdp(signal, true);
      case ICE_CANDIDATE -> {
        if (signal.descriptionId() == null) {
          throw new CallValidationException("Candidate description ID is required.");
        }
        if (signal.candidate() != null && signal.candidate().length() > MAX_CANDIDATE_LENGTH) {
          throw new CallValidationException("ICE candidate is too large.");
        }
      }
    }
  }

  private static void requireSdp(CallSignal signal, boolean requiresDescriptionId) {
    if (signal.sdp() == null || signal.sdp().isBlank() || signal.sdp().length() > MAX_SDP_LENGTH) {
      throw new CallValidationException("Session description is invalid.");
    }
    if (requiresDescriptionId && signal.descriptionId() == null) {
      throw new CallValidationException("Answer offer ID is required.");
    }
  }

  private record CheckedSignal(
      dev.amir.synapse.call.application.model.CallRuntimeState state, boolean duplicate) {}
}
