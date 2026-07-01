package dev.amir.synapse.messaging.infrastructure.adapter.out.persistence.repo;

import dev.amir.synapse.messaging.domain.model.Room;
import dev.amir.synapse.messaging.domain.port.out.ListRoomSummariesPort;
import dev.amir.synapse.messaging.domain.port.out.LoadRoomPort;
import dev.amir.synapse.messaging.domain.port.out.LoadRoomSummaryPort;
import dev.amir.synapse.messaging.domain.port.out.RoomSummaryPage;
import dev.amir.synapse.messaging.domain.port.out.RoomSummaryProjection;
import dev.amir.synapse.messaging.domain.port.out.RoomSummarySearchCriteria;
import dev.amir.synapse.messaging.domain.port.out.SaveRoomPort;
import dev.amir.synapse.messaging.infrastructure.adapter.out.persistence.model.RoomPersistenceMapper;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
public class RoomPortImpl
    implements LoadRoomPort, SaveRoomPort, ListRoomSummariesPort, LoadRoomSummaryPort {
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
  public RoomSummaryPage findRoomSummaries(RoomSummarySearchCriteria criteria) {
    var page =
        roomJpaRepository.findRoomSummariesByMember(
            criteria.userId(),
            criteria.type(),
            criteria.status(),
            PageRequest.of(criteria.page(), criteria.size()));

    return new RoomSummaryPage(
        page.getContent(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages());
  }

  @Override
  public Optional<RoomSummaryProjection> findRoomSummaryByIdForMember(UUID roomId, UUID userId) {
    return roomJpaRepository.findRoomSummaryByIdForMember(roomId, userId);
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
