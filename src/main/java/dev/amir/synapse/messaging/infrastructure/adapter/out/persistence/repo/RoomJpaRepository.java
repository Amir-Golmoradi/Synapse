package dev.amir.synapse.messaging.infrastructure.adapter.out.persistence.repo;

import dev.amir.synapse.messaging.domain.enums.RoomStatus;
import dev.amir.synapse.messaging.domain.enums.RoomType;
import dev.amir.synapse.messaging.domain.port.out.RoomSummaryProjection;
import dev.amir.synapse.messaging.infrastructure.adapter.out.persistence.model.RoomJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
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
                       JOIN r.members m
                       WHERE r.id = :roomId
                       AND m.userId = :userId
                       AND r.status = dev.amir.synapse.messaging.domain.enums.RoomStatus.ACTIVE
                   """)
  boolean existsActiveRoomMembership(@Param("roomId") UUID roomId, @Param("userId") UUID userId);

  @Query(
      value =
          """
                         SELECT new dev.amir.synapse.messaging.domain.port.out.RoomSummaryProjection(
                           r.id,
                           r.roomType,
                           r.status,
                           r.name,
                           r.avatarUrl,
                           SIZE(r.members),
                           r.createdAt,
                           r.lastMessagesAt
                         )
                         FROM RoomJpaEntity r
                         JOIN r.members m
                         WHERE m.userId = :userId
                           AND r.status = :status
                           AND (:type IS NULL OR r.roomType = :type)
                         ORDER BY r.lastMessagesAt DESC
                     """,
      countQuery =
          """
                         SELECT COUNT(r)
                         FROM RoomJpaEntity r
                         JOIN r.members m
                         WHERE m.userId = :userId
                           AND r.status = :status
                           AND (:type IS NULL OR r.roomType = :type)
                     """)
  Page<RoomSummaryProjection> findRoomSummariesByMember(
      @Param("userId") UUID userId,
      @Param("type") RoomType type,
      @Param("status") RoomStatus status,
      Pageable pageable);

  @Query(
      """
                         SELECT new dev.amir.synapse.messaging.domain.port.out.RoomSummaryProjection(
                           r.id,
                           r.roomType,
                           r.status,
                           r.name,
                           r.avatarUrl,
                           SIZE(r.members),
                           r.createdAt,
                           r.lastMessagesAt
                         )
                         FROM RoomJpaEntity r
                         JOIN r.members m
                         WHERE r.id = :roomId
                           AND m.userId = :userId
                     """)
  Optional<RoomSummaryProjection> findRoomSummaryByIdForMember(
      @Param("roomId") UUID roomId, @Param("userId") UUID userId);
}
