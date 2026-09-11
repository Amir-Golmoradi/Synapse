package dev.amir.synapse.call.infrastructure.adapter.in.web.rest;

import dev.amir.synapse.call.domain.port.in.AcceptCallUseCase;
import dev.amir.synapse.call.domain.port.in.CallView;
import dev.amir.synapse.call.domain.port.in.EndCallUseCase;
import dev.amir.synapse.call.domain.port.in.RejectCallUseCase;
import dev.amir.synapse.call.domain.port.in.ResumeCallUseCase;
import dev.amir.synapse.call.domain.port.in.StartCallUseCase;
import dev.amir.synapse.call.infrastructure.adapter.in.web.dto.AcceptCallRequest;
import dev.amir.synapse.call.infrastructure.adapter.in.web.dto.EndCallRequest;
import dev.amir.synapse.call.infrastructure.adapter.in.web.dto.ResumeCallRequest;
import dev.amir.synapse.call.infrastructure.adapter.in.web.dto.StartCallRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/calls")
public class CallCommandApi {
  private final StartCallUseCase start;
  private final AcceptCallUseCase accept;
  private final RejectCallUseCase reject;
  private final EndCallUseCase end;
  private final ResumeCallUseCase resume;

  public CallCommandApi(
      StartCallUseCase start,
      AcceptCallUseCase accept,
      RejectCallUseCase reject,
      EndCallUseCase end,
      ResumeCallUseCase resume) {
    this.start = start;
    this.accept = accept;
    this.reject = reject;
    this.end = end;
    this.resume = resume;
  }

  @PostMapping
  public ResponseEntity<CallView> start(
      @Valid @RequestBody StartCallRequest request, Authentication authentication) {
    var result =
        start.handle(
            new StartCallUseCase.Command(
                actor(authentication),
                request.calleeId(),
                request.clientRequestId(),
                request.clientInstanceId()));
    return result.created()
        ? ResponseEntity.created(URI.create("/api/v1/calls/" + result.call().callId()))
            .body(result.call())
        : ResponseEntity.ok(result.call());
  }

  @PostMapping("/{callId}/accept")
  public CallView accept(
      @PathVariable UUID callId,
      @Valid @RequestBody AcceptCallRequest request,
      Authentication authentication) {
    return accept.accept(callId, actor(authentication), request.clientInstanceId());
  }

  @PostMapping("/{callId}/reject")
  public CallView reject(@PathVariable UUID callId, Authentication authentication) {
    return reject.reject(callId, actor(authentication));
  }

  @PostMapping("/{callId}/end")
  public CallView end(
      @PathVariable UUID callId,
      @Valid @RequestBody EndCallRequest request,
      Authentication authentication) {
    return end.end(callId, actor(authentication), request.reason());
  }

  @PostMapping("/{callId}/resume")
  public CallView resume(
      @PathVariable UUID callId,
      @Valid @RequestBody ResumeCallRequest request,
      Authentication authentication) {
    return resume.resume(
        new ResumeCallUseCase.Command(
            callId,
            actor(authentication),
            request.clientInstanceId(),
            request.requestId(),
            request.observedGeneration(),
            request.peerConnectionRetained()));
  }

  static UUID actor(Authentication authentication) {
    return UUID.fromString(authentication.getName());
  }
}
