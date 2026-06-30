package dev.amir.synapse.messaging.domain.port.in.list_room_inbox;

@FunctionalInterface
public interface ListRoomInboxUseCase {
  ListRoomInboxResponse handle(ListRoomInboxQuery query);
}
