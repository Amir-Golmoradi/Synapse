package dev.amir.synapse.messaging.application.query.list_room_inbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.amir.synapse.messaging.domain.enums.RoomStatus;
import dev.amir.synapse.messaging.domain.enums.RoomType;
import dev.amir.synapse.messaging.domain.port.in.list_room_inbox.ListRoomInboxQuery;
import dev.amir.synapse.messaging.domain.port.out.ListRoomSummariesPort;
import dev.amir.synapse.messaging.domain.port.out.RoomSummaryPage;
import dev.amir.synapse.messaging.domain.port.out.RoomSummaryProjection;
import dev.amir.synapse.messaging.domain.port.out.RoomSummarySearchCriteria;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ListRoomInboxHandlerTest {

  @Test
  void mapsSearchCriteriaAndProjectionPageToInboxResponse() {
    var port = mock(ListRoomSummariesPort.class);
    var handler = new ListRoomInboxHandler(port);
    var userId = UUID.randomUUID();
    var roomId = UUID.randomUUID();
    var createdAt = Instant.parse("2026-06-30T10:00:00Z");
    var lastMessagesAt = Instant.parse("2026-06-30T10:05:00Z");
    var query = new ListRoomInboxQuery(userId, RoomType.GROUP, RoomStatus.ACTIVE, 1, 10);
    var criteria = new RoomSummarySearchCriteria(userId, RoomType.GROUP, RoomStatus.ACTIVE, 1, 10);

    when(port.findRoomSummaries(criteria))
        .thenReturn(
            new RoomSummaryPage(
                List.of(
                    new RoomSummaryProjection(
                        roomId,
                        RoomType.GROUP,
                        RoomStatus.ACTIVE,
                        "Engineering",
                        null,
                        3,
                        createdAt,
                        lastMessagesAt)),
                1,
                10,
                11,
                2));

    var response = handler.handle(query);

    verify(port).findRoomSummaries(criteria);
    assertThat(response.page()).isEqualTo(1);
    assertThat(response.size()).isEqualTo(10);
    assertThat(response.totalElements()).isEqualTo(11);
    assertThat(response.totalPages()).isEqualTo(2);
    assertThat(response.items())
        .singleElement()
        .satisfies(
            summary -> {
              assertThat(summary.roomId()).isEqualTo(roomId);
              assertThat(summary.type()).isEqualTo(RoomType.GROUP);
              assertThat(summary.status()).isEqualTo(RoomStatus.ACTIVE);
              assertThat(summary.name()).isEqualTo("Engineering");
              assertThat(summary.memberCount()).isEqualTo(3);
              assertThat(summary.createdAt()).isEqualTo(createdAt);
              assertThat(summary.lastMessagesAt()).isEqualTo(lastMessagesAt);
            });
  }
}
