package dev.amir.synapse.call.infrastructure.adapter.in.web.rest;

import dev.amir.synapse.call.domain.port.in.CallView;
import dev.amir.synapse.call.domain.port.in.GetCallUseCase;
import dev.amir.synapse.call.domain.port.in.GetCurrentCallUseCase;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/calls")
public class CallQueryApi {
  private final GetCallUseCase getCall;
  private final GetCurrentCallUseCase getCurrentCall;

  public CallQueryApi(GetCallUseCase getCall, GetCurrentCallUseCase getCurrentCall) {
    this.getCall = getCall;
    this.getCurrentCall = getCurrentCall;
  }

  @GetMapping("/{callId}")
  public CallView get(@PathVariable UUID callId, Authentication authentication) {
    return getCall.get(callId, CallCommandApi.actor(authentication));
  }

  @GetMapping("/current")
  public ResponseEntity<CallView> current(Authentication authentication) {
    return getCurrentCall
        .getCurrent(CallCommandApi.actor(authentication))
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.noContent().build());
  }
}
