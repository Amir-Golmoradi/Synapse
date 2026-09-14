package dev.amir.synapse.messaging.infrastructure.adapter.in.web.rest;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import dev.amir.synapse.messaging.domain.enums.RoomStatus;
import dev.amir.synapse.messaging.domain.enums.RoomType;
import dev.amir.synapse.messaging.domain.port.in.get_room_by_id.GetRoomByIdQuery;
import dev.amir.synapse.messaging.domain.port.in.get_room_by_id.GetRoomByIdUseCase;
import dev.amir.synapse.messaging.domain.port.in.list_room_inbox.ListRoomInboxQuery;
import dev.amir.synapse.messaging.domain.port.in.list_room_inbox.ListRoomInboxResponse;
import dev.amir.synapse.messaging.domain.port.in.list_room_inbox.ListRoomInboxUseCase;
import dev.amir.synapse.messaging.domain.port.in.list_room_inbox.RoomSummary;
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
public class RoomQueryApi {
  private static final int INBOX_PAGE_SIZE = 10;

  private final ListRoomInboxUseCase listRoomInboxUseCase;
  private final GetRoomByIdUseCase getRoomByIdUseCase;

  public RoomQueryApi(
      ListRoomInboxUseCase listRoomInboxUseCase, GetRoomByIdUseCase getRoomByIdUseCase) {
    this.listRoomInboxUseCase = listRoomInboxUseCase;
    this.getRoomByIdUseCase = getRoomByIdUseCase;
  }

  @GetMapping("/inbox")
  public ResponseEntity<ListRoomInboxResponse> inbox(
      Authentication authentication,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(required = false) @Nullable RoomType type,
      @RequestParam(required = false) @Nullable RoomStatus status) {
    var userId = UUID.fromString(authentication.getName());
    var effectiveStatus = status != null ? status : RoomStatus.ACTIVE;
    var query = new ListRoomInboxQuery(userId, type, effectiveStatus, page, INBOX_PAGE_SIZE);

    return ResponseEntity.ok(listRoomInboxUseCase.handle(query));
  }

  @GetMapping("/{roomId}")
  public ResponseEntity<RoomSummary> getById(
      Authentication authentication, @PathVariable UUID roomId) {
    var userId = UUID.fromString(authentication.getName());
    var query = new GetRoomByIdQuery(userId, roomId);

    return getRoomByIdUseCase
        .handle(query)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }
}
