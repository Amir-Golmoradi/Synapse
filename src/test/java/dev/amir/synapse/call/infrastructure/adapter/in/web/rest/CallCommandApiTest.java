package dev.amir.synapse.call.infrastructure.adapter.in.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.amir.synapse.call.domain.enums.CallStatus;
import dev.amir.synapse.call.domain.port.in.AcceptCallUseCase;
import dev.amir.synapse.call.domain.port.in.CallView;
import dev.amir.synapse.call.domain.port.in.EndCallUseCase;
import dev.amir.synapse.call.domain.port.in.RejectCallUseCase;
import dev.amir.synapse.call.domain.port.in.ResumeCallUseCase;
import dev.amir.synapse.call.domain.port.in.StartCallUseCase;
import dev.amir.synapse.identity.domain.port.in.access_token.AuthenticateAccessTokenUseCase;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CallCommandApi.class)
@AutoConfigureMockMvc(addFilters = false)
class CallCommandApiTest {
  @Autowired private MockMvc mockMvc;
  @MockitoBean private StartCallUseCase start;
  @MockitoBean private AcceptCallUseCase accept;
  @MockitoBean private RejectCallUseCase reject;
  @MockitoBean private EndCallUseCase end;
  @MockitoBean private ResumeCallUseCase resume;
  @MockitoBean private AuthenticateAccessTokenUseCase authenticateAccessToken;

  @Test
  void startDerivesCallerFromPrincipalAndReturnsLocation() throws Exception {
    var caller = UUID.randomUUID();
    var callee = UUID.randomUUID();
    var requestId = UUID.randomUUID();
    var instanceId = UUID.randomUUID();
    var callId = UUID.randomUUID();
    var view = view(callId, caller, callee);
    when(start.handle(any())).thenReturn(new StartCallUseCase.Result(view, true));

    mockMvc
        .perform(
            post("/api/v1/calls")
                .principal(authenticated(caller))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"calleeId":"%s","clientRequestId":"%s","clientInstanceId":"%s"}
                    """
                        .formatted(callee, requestId, instanceId)))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/v1/calls/" + callId))
        .andExpect(jsonPath("$.callId").value(callId.toString()))
        .andExpect(jsonPath("$.status").value("RINGING"));

    var captor = ArgumentCaptor.forClass(StartCallUseCase.Command.class);
    verify(start).handle(captor.capture());
    assertThat(captor.getValue().callerId()).isEqualTo(caller);
    assertThat(captor.getValue().calleeId()).isEqualTo(callee);
  }

  private static UsernamePasswordAuthenticationToken authenticated(UUID userId) {
    return UsernamePasswordAuthenticationToken.authenticated(userId.toString(), null, List.of());
  }

  private static CallView view(UUID callId, UUID caller, UUID callee) {
    var now = Instant.parse("2026-09-10T10:00:00Z");
    return new CallView(
        callId,
        caller,
        callee,
        CallStatus.RINGING,
        0,
        now,
        now,
        null,
        null,
        null,
        now.plusSeconds(45),
        null,
        null,
        0);
  }
}
