package dev.amir.synapse.messaging.infrastructure.adapter.in.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.amir.synapse.identity.domain.port.in.access_token.AuthenticateAccessTokenUseCase;
import dev.amir.synapse.messaging.domain.enums.RoomStatus;
import dev.amir.synapse.messaging.domain.enums.RoomType;
import dev.amir.synapse.messaging.domain.port.in.get_room_by_id.GetRoomByIdQuery;
import dev.amir.synapse.messaging.domain.port.in.get_room_by_id.GetRoomByIdUseCase;
import dev.amir.synapse.messaging.domain.port.in.list_room_inbox.ListRoomInboxQuery;
import dev.amir.synapse.messaging.domain.port.in.list_room_inbox.ListRoomInboxResponse;
import dev.amir.synapse.messaging.domain.port.in.list_room_inbox.ListRoomInboxUseCase;
import dev.amir.synapse.messaging.domain.port.in.list_room_inbox.RoomSummary;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RoomQueryApi.class)
@AutoConfigureMockMvc(addFilters = false)
class RoomQueryApiTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ListRoomInboxUseCase listRoomInboxUseCase;

  @MockitoBean private GetRoomByIdUseCase getRoomByIdUseCase;

  @MockitoBean private AuthenticateAccessTokenUseCase authenticateAccessTokenUseCase;

  @Test
  void inboxDefaultsToActiveRoomsWithFixedPageSize() throws Exception {
    var userId = UUID.randomUUID();
    var roomId = UUID.randomUUID();
    var createdAt = Instant.parse("2026-06-30T10:00:00Z");
    var lastMessagesAt = Instant.parse("2026-06-30T10:05:00Z");
    when(listRoomInboxUseCase.handle(any(ListRoomInboxQuery.class)))
        .thenReturn(
            new ListRoomInboxResponse(
                List.of(
                    new RoomSummary(
                        roomId,
                        RoomType.GROUP,
                        RoomStatus.ACTIVE,
                        "Engineering",
                        null,
                        2,
                        createdAt,
                        lastMessagesAt)),
                0,
                10,
                1,
                1));

    mockMvc
        .perform(get("/api/v1/room/inbox").principal(authenticated(userId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].roomId").value(roomId.toString()))
        .andExpect(jsonPath("$.items[0].type").value("GROUP"))
        .andExpect(jsonPath("$.items[0].status").value("ACTIVE"))
        .andExpect(jsonPath("$.items[0].name").value("Engineering"))
        .andExpect(jsonPath("$.items[0].memberCount").value(2))
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(10))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.totalPages").value(1));

    var captor = ArgumentCaptor.forClass(ListRoomInboxQuery.class);
    verify(listRoomInboxUseCase).handle(captor.capture());
    assertThat(captor.getValue())
        .satisfies(
            query -> {
              assertThat(query.userId()).isEqualTo(userId);
              assertThat(query.type()).isNull();
              assertThat(query.status()).isEqualTo(RoomStatus.ACTIVE);
              assertThat(query.page()).isZero();
              assertThat(query.size()).isEqualTo(10);
            });
  }

  @Test
  void inboxAcceptsPageTypeAndStatusFilters() throws Exception {
    var userId = UUID.randomUUID();
    when(listRoomInboxUseCase.handle(any(ListRoomInboxQuery.class)))
        .thenReturn(new ListRoomInboxResponse(List.of(), 2, 10, 0, 0));

    mockMvc
        .perform(
            get("/api/v1/room/inbox")
                .param("page", "2")
                .param("type", "CHANNEL")
                .param("status", "ARCHIVED")
                .principal(authenticated(userId)))
        .andExpect(status().isOk());

    var captor = ArgumentCaptor.forClass(ListRoomInboxQuery.class);
    verify(listRoomInboxUseCase).handle(captor.capture());
    assertThat(captor.getValue())
        .satisfies(
            query -> {
              assertThat(query.userId()).isEqualTo(userId);
              assertThat(query.type()).isEqualTo(RoomType.CHANNEL);
              assertThat(query.status()).isEqualTo(RoomStatus.ARCHIVED);
              assertThat(query.page()).isEqualTo(2);
              assertThat(query.size()).isEqualTo(10);
            });
  }

  @Test
  void getByIdReturnsMemberScopedRoomSummary() throws Exception {
    var userId = UUID.randomUUID();
    var roomId = UUID.randomUUID();
    var createdAt = Instant.parse("2026-06-30T10:00:00Z");
    var lastMessagesAt = Instant.parse("2026-06-30T10:05:00Z");
    when(getRoomByIdUseCase.handle(any(GetRoomByIdQuery.class)))
        .thenReturn(
            Optional.of(
                new RoomSummary(
                    roomId,
                    RoomType.GROUP,
                    RoomStatus.ARCHIVED,
                    "Engineering",
                    null,
                    2,
                    createdAt,
                    lastMessagesAt)));

    mockMvc
        .perform(get("/api/v1/room/{roomId}", roomId).principal(authenticated(userId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.roomId").value(roomId.toString()))
        .andExpect(jsonPath("$.type").value("GROUP"))
        .andExpect(jsonPath("$.status").value("ARCHIVED"))
        .andExpect(jsonPath("$.name").value("Engineering"))
        .andExpect(jsonPath("$.memberCount").value(2));

    var captor = ArgumentCaptor.forClass(GetRoomByIdQuery.class);
    verify(getRoomByIdUseCase).handle(captor.capture());
    assertThat(captor.getValue())
        .satisfies(
            query -> {
              assertThat(query.userId()).isEqualTo(userId);
              assertThat(query.roomId()).isEqualTo(roomId);
            });
  }

  @Test
  void getByIdReturnsNotFoundWhenRoomIsMissingOrUserIsNotMember() throws Exception {
    var userId = UUID.randomUUID();
    var roomId = UUID.randomUUID();
    when(getRoomByIdUseCase.handle(any(GetRoomByIdQuery.class))).thenReturn(Optional.empty());

    mockMvc
        .perform(get("/api/v1/room/{roomId}", roomId).principal(authenticated(userId)))
        .andExpect(status().isNotFound());
  }

  @Test
  void roomAccessDeniedMapsToSanitizedForbiddenProblem() throws Exception {
    var userId = UUID.randomUUID();
    var rawMessage =
        "Access Denied: User [%s] is not a member of Room [%s]."
            .formatted(userId, UUID.randomUUID());
    when(listRoomInboxUseCase.handle(any(ListRoomInboxQuery.class)))
        .thenThrow(new SecurityException(rawMessage));

    var response =
        mockMvc
            .perform(get("/api/v1/room/inbox").principal(authenticated(userId)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.title").value("Room access denied"))
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.detail").value("You are not allowed to access this room."))
            .andExpect(jsonPath("$.errorCode").value("ROOM_ACCESS_DENIED"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(response).doesNotContain(rawMessage).doesNotContain(userId.toString());
  }

  private static UsernamePasswordAuthenticationToken authenticated(UUID userId) {
    return new UsernamePasswordAuthenticationToken(userId.toString(), null, List.of());
  }
}
