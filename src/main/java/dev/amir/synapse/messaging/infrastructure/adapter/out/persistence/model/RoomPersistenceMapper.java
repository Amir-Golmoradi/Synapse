package dev.amir.synapse.messaging.infrastructure.adapter.out.persistence.model;

import dev.amir.synapse.messaging.domain.model.Room;
import dev.amir.synapse.messaging.domain.model.RoomSnapshot;
import dev.amir.synapse.messaging.domain.value_object.MemberId;
import dev.amir.synapse.messaging.domain.value_object.RoomId;
import java.util.Set;
import java.util.UUID;
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
            toUserIds(entity.getMemberIds()));
    return Room.reconstitute(state);
  }

  public RoomJpaEntity toEntity(Room room) {
    return RoomJpaEntity.create(
        room.getId().getValue(),
        room.getRoomType(),
        room.getName(),
        room.getAvatarUrl(),
        toUuids(room.getMembers()));
  }

  private Set<MemberId> toUserIds(Set<UUID> memberIds) {
    return memberIds.stream().map(MemberId::new).collect(Collectors.toUnmodifiableSet());
  }

  private Set<UUID> toUuids(Set<MemberId> members) {
    return members.stream().map(MemberId::getValue).collect(Collectors.toUnmodifiableSet());
  }
}
