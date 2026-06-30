package dev.amir.synapse.messaging.application.query.list_room_inbox;

import dev.amir.synapse.messaging.domain.port.in.list_room_inbox.ListRoomInboxQuery;
import dev.amir.synapse.messaging.domain.port.in.list_room_inbox.ListRoomInboxResponse;
import dev.amir.synapse.messaging.domain.port.in.list_room_inbox.ListRoomInboxUseCase;
import dev.amir.synapse.messaging.domain.port.in.list_room_inbox.RoomSummary;
import dev.amir.synapse.messaging.domain.port.out.ListRoomSummariesPort;
import dev.amir.synapse.messaging.domain.port.out.RoomSummaryProjection;
import dev.amir.synapse.messaging.domain.port.out.RoomSummarySearchCriteria;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListRoomInboxHandler implements ListRoomInboxUseCase {
  private final ListRoomSummariesPort listRoomSummariesPort;

  public ListRoomInboxHandler(ListRoomSummariesPort listRoomSummariesPort) {
    this.listRoomSummariesPort = listRoomSummariesPort;
  }

  @Override
  @Transactional(readOnly = true)
  public ListRoomInboxResponse handle(ListRoomInboxQuery query) {
    var criteria =
        new RoomSummarySearchCriteria(
            query.userId(), query.type(), query.status(), query.page(), query.size());
    var page = listRoomSummariesPort.findRoomSummaries(criteria);
    return new ListRoomInboxResponse(
        page.items().stream().map(ListRoomInboxHandler::toSummary).toList(),
        page.page(),
        page.size(),
        page.totalElements(),
        page.totalPages());
  }

  private static RoomSummary toSummary(RoomSummaryProjection projection) {
    return new RoomSummary(
        projection.roomId(),
        projection.type(),
        projection.status(),
        projection.name(),
        projection.avatarUrl(),
        projection.memberCount(),
        projection.createdAt(),
        projection.lastMessagesAt());
  }
}
