package dev.amir.synapse.messaging.infrastructure.adapter.out.persistence.repo;

import dev.amir.synapse.messaging.domain.enums.RoomStatus;
import dev.amir.synapse.messaging.domain.model.Room;
import dev.amir.synapse.messaging.domain.port.out.LoadRoomPort;
import dev.amir.synapse.messaging.domain.port.out.SaveRoomPort;
import dev.amir.synapse.messaging.infrastructure.adapter.out.persistence.model.RoomPersistenceMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public class RoomPortImpl implements LoadRoomPort, SaveRoomPort {
  private final RoomPersistenceMapper mapper;
  private final RoomJpaRepository roomJpaRepository;

  public RoomPortImpl(RoomPersistenceMapper mapper, RoomJpaRepository roomJpaRepository) {
    this.mapper = mapper;
    this.roomJpaRepository = roomJpaRepository;
  }

  @Override
  public Optional<Room> findById(UUID roomId) {
    return roomJpaRepository.findById(roomId).stream().map(mapper::toDomain).findFirst();
  }

  @Override
  public List<Room> findActiveRoomsForUser(UUID userId) {
    return roomJpaRepository
        .findByStatusAndMemberIdsContaining(RoomStatus.ACTIVE, userId, Pageable.unpaged())
        .stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public boolean hasActiveMembership(UUID roomId, UUID userId) {
    return roomJpaRepository.existsActiveRoomMembership(roomId, userId);
  }

  @Override
  public Room save(Room room) {
    var roomJpaEntity = mapper.toEntity(room);
    var savedRoom = roomJpaRepository.save(roomJpaEntity);
    return mapper.toDomain(savedRoom);
  }
}
