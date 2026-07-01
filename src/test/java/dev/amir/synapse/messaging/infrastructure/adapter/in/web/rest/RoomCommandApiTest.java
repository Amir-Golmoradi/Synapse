package dev.amir.synapse.messaging.infrastructure.adapter.in.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.amir.synapse.identity.domain.port.in.access_token.AuthenticateAccessTokenUseCase;
import dev.amir.synapse.messaging.application.command.create_direct_room.exception.CannotCreateDirectMessageWithSelfException;
import dev.amir.synapse.messaging.application.command.create_direct_room.exception.DirectMessageRecipientNotFoundException;
import dev.amir.synapse.messaging.application.command.create_group_room.exception.GroupMembersNotFoundException;
import dev.amir.synapse.messaging.domain.exception.RoomValidationException;
import dev.amir.synapse.messaging.domain.port.in.create_channel_room.CreateChannelUseCase;
import dev.amir.synapse.messaging.domain.port.in.create_direct_room.CreateDirectRoomUseCase;
import dev.amir.synapse.messaging.domain.port.in.create_group_room.CreateGroupUseCase;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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

  @Test
  void directRoomWithSelfMapsToBadRequestProblem() throws Exception {
    var creatorId = UUID.randomUUID();
    var rawMessage = "Cannot create a direct message with self";
    when(directRoomUseCase.handle(any()))
        .thenThrow(new CannotCreateDirectMessageWithSelfException());

    var response =
        mockMvc
            .perform(
                post("/api/v1/room/direct")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"recipientId":"%s"}
                        """
                            .formatted(creatorId))
                    .principal(authenticated(creatorId)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Direct message is invalid"))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.detail").value("A direct room requires two different users."))
            .andExpect(jsonPath("$.errorCode").value("DIRECT_MESSAGE_WITH_SELF"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(response).doesNotContain(rawMessage);
  }

  @Test
  void missingDirectRecipientMapsToNotFoundProblem() throws Exception {
    var creatorId = UUID.randomUUID();
    var recipientId = UUID.randomUUID();
    when(directRoomUseCase.handle(any())).thenThrow(new DirectMessageRecipientNotFoundException());

    var response =
        mockMvc
            .perform(
                post("/api/v1/room/direct")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"recipientId":"%s"}
                        """
                            .formatted(recipientId))
                    .principal(authenticated(creatorId)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Room participant not found"))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(
                jsonPath("$.detail")
                    .value("One or more requested room participants were not found."))
            .andExpect(jsonPath("$.errorCode").value("ROOM_PARTICIPANT_NOT_FOUND"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(response).doesNotContain("Direct message recipient not found");
  }

  @Test
  void missingGroupMembersMapsToSanitizedNotFoundProblem() throws Exception {
    var creatorId = UUID.randomUUID();
    var missingMemberId = UUID.randomUUID();
    when(groupUseCase.handle(any()))
        .thenThrow(new GroupMembersNotFoundException(Set.of(missingMemberId)));

    var response =
        mockMvc
            .perform(
                post("/api/v1/room/group")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"name":"Engineering","initialMemberIds":["%s"]}
                        """
                            .formatted(missingMemberId))
                    .principal(authenticated(creatorId)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Room participant not found"))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.errorCode").value("ROOM_PARTICIPANT_NOT_FOUND"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(response).doesNotContain(missingMemberId.toString());
  }

  @Test
  void roomValidationMapsToBadRequestProblem() throws Exception {
    var creatorId = UUID.randomUUID();
    var rawMessage = "Room name cannot be blank.";
    when(groupUseCase.handle(any())).thenThrow(new RoomValidationException(rawMessage));

    var response =
        mockMvc
            .perform(
                post("/api/v1/room/group")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"name":"Engineering","initialMemberIds":[]}
                        """)
                    .principal(authenticated(creatorId)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Room validation failed"))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.detail").value("The room request violates a room rule."))
            .andExpect(jsonPath("$.errorCode").value("ROOM_VALIDATION_FAILED"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(response).doesNotContain(rawMessage);
  }

  private static UsernamePasswordAuthenticationToken authenticated(UUID userId) {
    return new UsernamePasswordAuthenticationToken(userId, null, List.of());
  }
}
