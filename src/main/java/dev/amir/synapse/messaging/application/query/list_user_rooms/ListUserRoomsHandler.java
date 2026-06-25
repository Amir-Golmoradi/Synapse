package dev.amir.synapse.messaging.application.query.list_user_rooms;

import dev.amir.synapse.messaging.domain.port.in.list_user_rooms.ListUserRoomsQuery;
import dev.amir.synapse.messaging.domain.port.in.list_user_rooms.ListUserRoomsResult;
import dev.amir.synapse.messaging.domain.port.in.list_user_rooms.ListUserRoomsUseCase;
import dev.amir.synapse.messaging.domain.port.out.LoadRoomPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListUserRoomsHandler implements ListUserRoomsUseCase {
  private final LoadRoomPort loadRoomPort;

  public ListUserRoomsHandler(LoadRoomPort loadRoomPort) {
    this.loadRoomPort = loadRoomPort;
  }

  @Override
  @Transactional
  public ListUserRoomsResult handle(ListUserRoomsQuery query) {
    var rooms = loadRoomPort.findActiveRoomsForUser(query.userId());
    return new ListUserRoomsResult(rooms);
  }
}
