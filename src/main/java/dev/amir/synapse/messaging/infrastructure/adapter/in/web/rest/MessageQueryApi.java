package dev.amir.synapse.messaging.infrastructure.adapter.in.web.rest;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import dev.amir.synapse.messaging.domain.port.in.list_messages.ListMessagesQuery;
import dev.amir.synapse.messaging.domain.port.in.list_messages.ListMessagesUseCase;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping(value = "api/v1/room", produces = APPLICATION_JSON_VALUE)
public class MessageQueryApi {
  private static final String DEFAULT_LIMIT = "50";

  private final ListMessagesUseCase listMessagesUseCase;
  private final MessageCursorCodec cursorCodec;

  public MessageQueryApi(ListMessagesUseCase listMessagesUseCase, MessageCursorCodec cursorCodec) {
    this.listMessagesUseCase = listMessagesUseCase;
    this.cursorCodec = cursorCodec;
  }

  @GetMapping("/{roomId}/messages")
  public ResponseEntity<MessageHistoryResponse> listMessages(
      Authentication authentication,
      @PathVariable UUID roomId,
      @RequestParam(defaultValue = DEFAULT_LIMIT)
          @Min(ListMessagesQuery.MIN_LIMIT)
          @Max(ListMessagesQuery.MAX_LIMIT)
          int limit,
      @RequestParam(required = false) @Nullable String cursor) {
    var requesterId = UUID.fromString(authentication.getName());
    var decodedCursor = cursor == null ? null : cursorCodec.decode(roomId, cursor);
    var result =
        listMessagesUseCase.handle(
            new ListMessagesQuery(requesterId, roomId, limit, decodedCursor));
    var nextCursor =
        result.nextCursor() == null ? null : cursorCodec.encode(roomId, result.nextCursor());

    return ResponseEntity.ok(new MessageHistoryResponse(result.items(), nextCursor));
  }
}
