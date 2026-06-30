package dev.amir.synapse.messaging.infrastructure.adapter.out.persistence.model;

import dev.amir.synapse.messaging.domain.model.Room;
import dev.amir.synapse.messaging.domain.model.RoomSnapshot;
import dev.amir.synapse.messaging.domain.value_object.RoomId;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class RoomPersistenceMapper {

  public Room toDomain(RoomJpaEntity entity) {
    var state =
        new RoomSnapshot(
            RoomId.of(entity.getId()),
            entity.getRoomType(),
            entity.getName(),
            entity.getAvatarUrl(),
            entity.getStatus(),
            entity.getCreatedAt(),
            entity.getLastMessagesAt(),
            entity.getMembers().stream()
                .map(RoomMemberEmbeddable::toDomain)
                .collect(Collectors.toUnmodifiableSet()));

    return Room.reconstitute(state);
  }

  public RoomJpaEntity toEntity(Room room) {
    return RoomJpaEntity.fromDomainState(
        room.getId().getValue(),
        room.getRoomType(),
        room.getName(),
        room.getAvatarUrl(),
        room.getStatus(),
        room.getCreatedAt(),
        room.getLastMessagesAt(),
        room.getMembers().values().stream()
            .map(RoomMemberEmbeddable::fromDomain)
            .collect(Collectors.toUnmodifiableSet()));
  }
}
