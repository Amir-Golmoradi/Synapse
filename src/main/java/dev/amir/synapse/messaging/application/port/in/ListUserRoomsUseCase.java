package dev.amir.synapse.messaging.application.port.in;

import dev.amir.synapse.messaging.application.query.list_user_rooms.ListUserRoomsQuery;
import dev.amir.synapse.messaging.application.query.list_user_rooms.ListUserRoomsResult;

@FunctionalInterface
public interface ListUserRoomsUseCase {
  ListUserRoomsResult handle(ListUserRoomsQuery query);
}
