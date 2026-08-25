package dev.amir.synapse.messaging.infrastructure.adapter.in.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.amir.synapse.identity.domain.port.in.access_token.AuthenticateAccessTokenUseCase;
import dev.amir.synapse.messaging.domain.exception.MessageRoomAccessDeniedException;
import dev.amir.synapse.messaging.domain.port.in.list_messages.ListMessagesQuery;
import dev.amir.synapse.messaging.domain.port.in.list_messages.ListMessagesResult;
import dev.amir.synapse.messaging.domain.port.in.list_messages.ListMessagesUseCase;
import dev.amir.synapse.messaging.domain.port.in.list_messages.MessageCursor;
import dev.amir.synapse.messaging.domain.port.in.message.MessageView;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    value = MessageQueryApi.class,
    excludeAutoConfiguration = OAuth2ClientAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
class MessageQueryApiTest {
  @Autowired private MockMvc mockMvc;

  @MockitoBean private ListMessagesUseCase listMessagesUseCase;
  @MockitoBean private MessageCursorCodec cursorCodec;
  @MockitoBean private AuthenticateAccessTokenUseCase authenticateAccessTokenUseCase;

  @Test
  void returnsNewestFirstMessagesAndAnEncodedNextCursor() throws Exception {
    var requesterId = UUID.randomUUID();
    var roomId = UUID.randomUUID();
    var first = message(roomId, requesterId, "2026-08-13T10:05:00Z");
    var second = message(roomId, UUID.randomUUID(), "2026-08-13T10:04:00Z");
    var nextCursor = new MessageCursor(second.createdAt(), second.messageId());
    when(listMessagesUseCase.handle(any(ListMessagesQuery.class)))
        .thenReturn(new ListMessagesResult(List.of(first, second), nextCursor));
    when(cursorCodec.encode(roomId, nextCursor)).thenReturn("next-token");

    mockMvc
        .perform(
            get("/api/v1/room/{roomId}/messages", roomId).principal(authenticated(requesterId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].messageId").value(first.messageId().toString()))
        .andExpect(jsonPath("$.items[0].roomId").value(roomId.toString()))
        .andExpect(jsonPath("$.items[0].senderId").value(requesterId.toString()))
        .andExpect(jsonPath("$.items[0].clientMessageId").value(first.clientMessageId().toString()))
        .andExpect(jsonPath("$.items[0].text").value(first.text()))
        .andExpect(jsonPath("$.items[0].createdAt").value(first.createdAt().toString()))
        .andExpect(jsonPath("$.items[1].messageId").value(second.messageId().toString()))
        .andExpect(jsonPath("$.nextCursor").value("next-token"));

    var captor = ArgumentCaptor.forClass(ListMessagesQuery.class);
    verify(listMessagesUseCase).handle(captor.capture());
    assertThat(captor.getValue())
        .satisfies(
            query -> {
              assertThat(query.requesterId()).isEqualTo(requesterId);
              assertThat(query.roomId()).isEqualTo(roomId);
              assertThat(query.limit()).isEqualTo(50);
              assertThat(query.cursor()).isNull();
            });
  }

  @Test
  void decodesRoomScopedCursorAndAcceptsMaximumLimit() throws Exception {
    var requesterId = UUID.randomUUID();
    var roomId = UUID.randomUUID();
    var cursor = new MessageCursor(Instant.parse("2026-08-13T10:00:00Z"), UUID.randomUUID());
    when(cursorCodec.decode(roomId, "cursor-token")).thenReturn(cursor);
    when(listMessagesUseCase.handle(any(ListMessagesQuery.class)))
        .thenReturn(new ListMessagesResult(List.of(), null));

    mockMvc
        .perform(
            get("/api/v1/room/{roomId}/messages", roomId)
                .param("limit", "100")
                .param("cursor", "cursor-token")
                .principal(authenticated(requesterId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isEmpty())
        .andExpect(jsonPath("$.nextCursor").doesNotExist());

    var captor = ArgumentCaptor.forClass(ListMessagesQuery.class);
    verify(listMessagesUseCase).handle(captor.capture());
    assertThat(captor.getValue().limit()).isEqualTo(100);
    assertThat(captor.getValue().cursor()).isEqualTo(cursor);
  }

  @Test
  void invalidCursorReturnsSanitizedBadRequestWithoutQueryingHistory() throws Exception {
    var requesterId = UUID.randomUUID();
    var roomId = UUID.randomUUID();
    when(cursorCodec.decode(roomId, "invalid-token"))
        .thenThrow(new InvalidMessageCursorException());

    var response =
        mockMvc
            .perform(
                get("/api/v1/room/{roomId}/messages", roomId)
                    .param("cursor", "invalid-token")
                    .principal(authenticated(requesterId)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Message cursor is invalid"))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.detail").value("The message history cursor is invalid."))
            .andExpect(jsonPath("$.errorCode").value("MESSAGE_CURSOR_INVALID"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(response).doesNotContain("invalid-token");
    verifyNoInteractions(listMessagesUseCase);
  }

  @Test
  void inaccessibleAndMissingRoomsAreIndistinguishable() throws Exception {
    var requesterId = UUID.randomUUID();
    var roomId = UUID.randomUUID();
    when(listMessagesUseCase.handle(any(ListMessagesQuery.class)))
        .thenThrow(new MessageRoomAccessDeniedException());

    var response =
        mockMvc
            .perform(
                get("/api/v1/room/{roomId}/messages", roomId).principal(authenticated(requesterId)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Message room not found"))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.detail").value("The room was not found or is not accessible."))
            .andExpect(jsonPath("$.errorCode").value("MESSAGE_ROOM_NOT_FOUND"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(response).doesNotContain(requesterId.toString());
  }

  @Test
  void rejectsLimitsOutsideOneThroughOneHundredWithoutQueryingHistory() throws Exception {
    var requesterId = UUID.randomUUID();
    var roomId = UUID.randomUUID();

    for (var invalidLimit : List.of("0", "101")) {
      mockMvc
          .perform(
              get("/api/v1/room/{roomId}/messages", roomId)
                  .param("limit", invalidLimit)
                  .principal(authenticated(requesterId)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.title").value("Message history request is invalid"))
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.errorCode").value("MESSAGE_HISTORY_REQUEST_INVALID"));
    }

    verifyNoInteractions(listMessagesUseCase, cursorCodec);
  }

  private static UsernamePasswordAuthenticationToken authenticated(UUID userId) {
    return new UsernamePasswordAuthenticationToken(userId.toString(), null, List.of());
  }

  private static MessageView message(UUID roomId, UUID senderId, String createdAt) {
    return new MessageView(
        UUID.randomUUID(),
        roomId,
        senderId,
        UUID.randomUUID(),
        "A message",
        Instant.parse(createdAt));
  }
}
