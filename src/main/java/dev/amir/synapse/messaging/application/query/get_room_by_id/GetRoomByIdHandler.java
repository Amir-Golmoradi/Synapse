package dev.amir.synapse.messaging.application.query.get_room_by_id;

import dev.amir.synapse.messaging.domain.port.in.get_room_by_id.GetRoomByIdQuery;
import dev.amir.synapse.messaging.domain.port.in.get_room_by_id.GetRoomByIdUseCase;
import dev.amir.synapse.messaging.domain.port.in.list_room_inbox.RoomSummary;
import dev.amir.synapse.messaging.domain.port.out.LoadRoomSummaryPort;
import dev.amir.synapse.messaging.domain.port.out.RoomSummaryProjection;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetRoomByIdHandler implements GetRoomByIdUseCase {
  private final LoadRoomSummaryPort loadRoomSummaryPort;

  public GetRoomByIdHandler(LoadRoomSummaryPort loadRoomSummaryPort) {
    this.loadRoomSummaryPort = loadRoomSummaryPort;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<RoomSummary> handle(GetRoomByIdQuery query) {
    return loadRoomSummaryPort
        .findRoomSummaryByIdForMember(query.roomId(), query.userId())
        .map(GetRoomByIdHandler::toSummary);
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
