package dev.amir.synapse.messaging.application.query.list_user_rooms;

import dev.amir.synapse.messaging.domain.model.Room;
import java.util.List;

public record ListUserRoomsResult(List<Room> rooms) {}
