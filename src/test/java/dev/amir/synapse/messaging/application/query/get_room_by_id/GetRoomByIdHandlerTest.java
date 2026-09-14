package dev.amir.synapse.messaging.application.query.get_room_by_id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.amir.synapse.messaging.domain.enums.RoomStatus;
import dev.amir.synapse.messaging.domain.enums.RoomType;
import dev.amir.synapse.messaging.domain.port.in.get_room_by_id.GetRoomByIdQuery;
import dev.amir.synapse.messaging.domain.port.out.LoadRoomSummaryPort;
import dev.amir.synapse.messaging.domain.port.out.RoomSummaryProjection;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetRoomByIdHandlerTest {

  @Test
  void returnsMemberScopedRoomSummary() {
    var port = org.mockito.Mockito.mock(LoadRoomSummaryPort.class);
    var handler = new GetRoomByIdHandler(port);
    var userId = UUID.randomUUID();
    var roomId = UUID.randomUUID();
    var createdAt = Instant.parse("2026-06-30T10:00:00Z");
    var lastMessagesAt = Instant.parse("2026-06-30T10:05:00Z");
    when(port.findRoomSummaryByIdForMember(roomId, userId))
        .thenReturn(
            Optional.of(
                new RoomSummaryProjection(
                    roomId,
                    RoomType.GROUP,
                    RoomStatus.ARCHIVED,
                    "Engineering",
                    null,
                    2,
                    createdAt,
                    lastMessagesAt)));

    var result = handler.handle(new GetRoomByIdQuery(userId, roomId));

    assertThat(result)
        .hasValueSatisfying(
            summary -> {
              assertThat(summary.roomId()).isEqualTo(roomId);
              assertThat(summary.type()).isEqualTo(RoomType.GROUP);
              assertThat(summary.status()).isEqualTo(RoomStatus.ARCHIVED);
              assertThat(summary.name()).isEqualTo("Engineering");
              assertThat(summary.memberCount()).isEqualTo(2);
              assertThat(summary.createdAt()).isEqualTo(createdAt);
              assertThat(summary.lastMessagesAt()).isEqualTo(lastMessagesAt);
            });
    verify(port).findRoomSummaryByIdForMember(roomId, userId);
  }

  @Test
  void returnsEmptyWhenRoomIsMissingOrUserIsNotMember() {
    var port = org.mockito.Mockito.mock(LoadRoomSummaryPort.class);
    var handler = new GetRoomByIdHandler(port);
    var userId = UUID.randomUUID();
    var roomId = UUID.randomUUID();
    when(port.findRoomSummaryByIdForMember(roomId, userId)).thenReturn(Optional.empty());

    assertThat(handler.handle(new GetRoomByIdQuery(userId, roomId))).isEmpty();
  }
}
