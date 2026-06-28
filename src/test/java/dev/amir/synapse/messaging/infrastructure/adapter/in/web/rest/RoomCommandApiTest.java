package dev.amir.synapse.messaging.infrastructure.adapter.in.web.rest;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.amir.synapse.identity.domain.port.in.access_token.AuthenticateAccessTokenUseCase;
import dev.amir.synapse.messaging.domain.port.in.create_channel_room.CreateChannelUseCase;
import dev.amir.synapse.messaging.domain.port.in.create_direct_room.CreateDirectRoomUseCase;
import dev.amir.synapse.messaging.domain.port.in.create_group_room.CreateGroupUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RoomCommandApi.class)
@AutoConfigureMockMvc(addFilters = false)
class RoomCommandApiTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private CreateGroupUseCase groupUseCase;

  @MockitoBean private CreateChannelUseCase channelUseCase;

  @MockitoBean private CreateDirectRoomUseCase directRoomUseCase;

  @MockitoBean private AuthenticateAccessTokenUseCase authenticateAccessTokenUseCase;

  @Test
  void createDirectRoomRejectsMissingRecipientId() throws Exception {
    mockMvc
        .perform(post("/api/v1/room/direct").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(directRoomUseCase);
  }
}
