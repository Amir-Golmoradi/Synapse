package dev.amir.synapse.messaging.infrastructure.adapter.in.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.amir.synapse.identity.domain.port.in.access_token.AuthenticateAccessTokenUseCase;
import dev.amir.synapse.messaging.application.command.create_direct_room.exception.CannotCreateDirectMessageWithSelfException;
import dev.amir.synapse.messaging.application.command.create_direct_room.exception.DirectMessageRecipientNotFoundException;
import dev.amir.synapse.messaging.application.command.create_group_room.exception.GroupMembersNotFoundException;
import dev.amir.synapse.messaging.domain.exception.RoomValidationException;
import dev.amir.synapse.messaging.domain.port.in.create_channel_room.CreateChannelCommand;
import dev.amir.synapse.messaging.domain.port.in.create_channel_room.CreateChannelResponse;
import dev.amir.synapse.messaging.domain.port.in.create_channel_room.CreateChannelUseCase;
import dev.amir.synapse.messaging.domain.port.in.create_direct_room.CreateDirectRoomCommand;
import dev.amir.synapse.messaging.domain.port.in.create_direct_room.CreateDirectRoomResponse;
import dev.amir.synapse.messaging.domain.port.in.create_direct_room.CreateDirectRoomUseCase;
import dev.amir.synapse.messaging.domain.port.in.create_group_room.CreateGroupCommand;
import dev.amir.synapse.messaging.domain.port.in.create_group_room.CreateGroupResponse;
import dev.amir.synapse.messaging.domain.port.in.create_group_room.CreateGroupUseCase;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.OptimisticLockingFailureException;
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
  void createDirectRoomReturnsCreatedResponseShape() throws Exception {
    var creatorId = UUID.randomUUID();
    var recipientId = UUID.randomUUID();
    var roomId = UUID.randomUUID();
    var createdAt = Instant.parse("2026-06-30T10:00:00Z");
    when(directRoomUseCase.handle(any(CreateDirectRoomCommand.class)))
        .thenReturn(new CreateDirectRoomResponse(roomId, creatorId, recipientId, createdAt));

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
        .andExpect(status().isCreated())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.roomId").value(roomId.toString()))
        .andExpect(jsonPath("$.creatorId").value(creatorId.toString()))
        .andExpect(jsonPath("$.recipientId").value(recipientId.toString()))
        .andExpect(jsonPath("$.createdAt").value(createdAt.toString()));

    var captor = ArgumentCaptor.forClass(CreateDirectRoomCommand.class);
    verify(directRoomUseCase).handle(captor.capture());
    assertThat(captor.getValue())
        .satisfies(
            command -> {
              assertThat(command.creatorId()).isEqualTo(creatorId);
              assertThat(command.recipientId()).isEqualTo(recipientId);
            });
  }

  @Test
  void createGroupRoomReturnsCreatedResponseShape() throws Exception {
    var creatorId = UUID.randomUUID();
    var memberId = UUID.randomUUID();
    var groupId = UUID.randomUUID();
    var createdAt = Instant.parse("2026-06-30T10:00:00Z");
    var avatarUrl = "https://cdn.example/group.png";
    when(groupUseCase.handle(any(CreateGroupCommand.class)))
        .thenReturn(new CreateGroupResponse(groupId, "Engineering", avatarUrl, 2, createdAt));

    mockMvc
        .perform(
            post("/api/v1/room/group")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":"Engineering","avatarUrl":"%s","initialMemberIds":["%s"]}
                        """
                        .formatted(avatarUrl, memberId))
                .principal(authenticated(creatorId)))
        .andExpect(status().isCreated())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.groupId").value(groupId.toString()))
        .andExpect(jsonPath("$.name").value("Engineering"))
        .andExpect(jsonPath("$.avatarUrl").value(avatarUrl))
        .andExpect(jsonPath("$.members").value(2))
        .andExpect(jsonPath("$.createdAt").value(createdAt.toString()));

    var captor = ArgumentCaptor.forClass(CreateGroupCommand.class);
    verify(groupUseCase).handle(captor.capture());
    assertThat(captor.getValue())
        .satisfies(
            command -> {
              assertThat(command.creatorId()).isEqualTo(creatorId);
              assertThat(command.name()).isEqualTo("Engineering");
              assertThat(command.avatarUrl()).isEqualTo(avatarUrl);
              assertThat(command.initialMemberIds()).containsExactly(memberId);
            });
  }

  @Test
  void createChannelRoomReturnsCreatedResponseShape() throws Exception {
    var creatorId = UUID.randomUUID();
    var memberId = UUID.randomUUID();
    var channelId = UUID.randomUUID();
    var createdAt = Instant.parse("2026-06-30T10:00:00Z");
    var avatarUrl = "https://cdn.example/channel.png";
    when(channelUseCase.handle(any(CreateChannelCommand.class)))
        .thenReturn(new CreateChannelResponse(channelId, "Announcements", avatarUrl, 2, createdAt));

    mockMvc
        .perform(
            post("/api/v1/room/channel")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":"Announcements","avatarUrl":"%s","initialMemberIds":["%s"]}
                        """
                        .formatted(avatarUrl, memberId))
                .principal(authenticated(creatorId)))
        .andExpect(status().isCreated())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.channelId").value(channelId.toString()))
        .andExpect(jsonPath("$.name").value("Announcements"))
        .andExpect(jsonPath("$.avatarUrl").value(avatarUrl))
        .andExpect(jsonPath("$.memberCount").value(2))
        .andExpect(jsonPath("$.createdAt").value(createdAt.toString()));

    var captor = ArgumentCaptor.forClass(CreateChannelCommand.class);
    verify(channelUseCase).handle(captor.capture());
    assertThat(captor.getValue())
        .satisfies(
            command -> {
              assertThat(command.creatorId()).isEqualTo(creatorId);
              assertThat(command.name()).isEqualTo("Announcements");
              assertThat(command.avatarUrl()).isEqualTo(avatarUrl);
              assertThat(command.initialMemberIds()).containsExactly(memberId);
            });
  }

  @Test
  void createDirectRoomRejectsMissingRecipientId() throws Exception {
    mockMvc
        .perform(post("/api/v1/room/direct").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(directRoomUseCase);
  }

  @Test
  void createGroupRoomRejectsInvalidRequestPayloads() throws Exception {
    var creatorId = UUID.randomUUID();

    mockMvc
        .perform(
            post("/api/v1/room/group")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":"  ","initialMemberIds":[]}
                    """)
                .principal(authenticated(creatorId)))
        .andExpect(status().isBadRequest());
    mockMvc
        .perform(
            post("/api/v1/room/group")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"Engineering"}
                    """)
                .principal(authenticated(creatorId)))
        .andExpect(status().isBadRequest());
    mockMvc
        .perform(
            post("/api/v1/room/group")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":"Engineering","avatarUrl":"not-a-url","initialMemberIds":[]}
                    """)
                .principal(authenticated(creatorId)))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(groupUseCase);
  }

  @Test
  void createChannelRoomRejectsInvalidRequestPayloads() throws Exception {
    var creatorId = UUID.randomUUID();

    mockMvc
        .perform(
            post("/api/v1/room/channel")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":"  ","initialMemberIds":[]}
                    """)
                .principal(authenticated(creatorId)))
        .andExpect(status().isBadRequest());
    mockMvc
        .perform(
            post("/api/v1/room/channel")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"Announcements"}
                    """)
                .principal(authenticated(creatorId)))
        .andExpect(status().isBadRequest());
    mockMvc
        .perform(
            post("/api/v1/room/channel")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":"Announcements","avatarUrl":"not-a-url","initialMemberIds":[]}
                    """)
                .principal(authenticated(creatorId)))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(channelUseCase);
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

  @Test
  void optimisticLockingFailureMapsToSanitizedConflictProblem() throws Exception {
    var creatorId = UUID.randomUUID();
    var rawMessage = "stale room " + UUID.randomUUID();
    when(groupUseCase.handle(any())).thenThrow(new OptimisticLockingFailureException(rawMessage));

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
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.title").value("Room changed concurrently"))
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(
                jsonPath("$.detail")
                    .value("The room was changed by another request. Reload and retry."))
            .andExpect(jsonPath("$.errorCode").value("ROOM_CONCURRENT_MODIFICATION"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(response).doesNotContain(rawMessage);
  }

  private static UsernamePasswordAuthenticationToken authenticated(UUID userId) {
    return new UsernamePasswordAuthenticationToken(userId.toString(), null, List.of());
  }
}
