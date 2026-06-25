package dev.amir.synapse.messaging.infrastructure.adapter.out.persistence.repo;

import dev.amir.synapse.messaging.domain.enums.RoomStatus;
import dev.amir.synapse.messaging.infrastructure.adapter.out.persistence.model.RoomJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RoomJpaRepository extends JpaRepository<RoomJpaEntity, UUID> {
  @Query(
      """
    SELECT COUNT(r) > 0
    FROM RoomJpaEntity r
    JOIN r.memberIds memberId
    WHERE r.id = :roomId
    AND memberId = :userId
    AND r.status = dev.amir.synapse.messaging.domain.enums.RoomStatus.ACTIVE
""")
  boolean existsActiveRoomMembership(@Param("roomId") UUID roomId, @Param("userId") UUID userId);

  List<RoomJpaEntity> findByStatusAndMemberIdsContaining(
      RoomStatus status, UUID userId, Pageable pageable);
}
