package dev.amir.synapse.messaging.domain.port.in.list_user_rooms;

@FunctionalInterface
public interface ListUserRoomsUseCase {
  ListUserRoomsResult handle(ListUserRoomsQuery query);
}
