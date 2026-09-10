package dev.amir.synapse.call.infrastructure.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface CallJpaRepository extends JpaRepository<CallJpaEntity, UUID> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from CallJpaEntity c where c.id = :id")
  Optional<CallJpaEntity> findByIdForUpdate(@Param("id") UUID id);

  Optional<CallJpaEntity> findByCallerIdAndClientRequestId(UUID callerId, UUID clientRequestId);

  @Query(
      """
      select c from CallJpaEntity c
      where (c.callerId = :userId or c.calleeId = :userId)
        and c.status in (
          dev.amir.synapse.call.domain.enums.CallStatus.RINGING,
          dev.amir.synapse.call.domain.enums.CallStatus.CONNECTING,
          dev.amir.synapse.call.domain.enums.CallStatus.ACTIVE,
          dev.amir.synapse.call.domain.enums.CallStatus.RECOVERING)
      """)
  Optional<CallJpaEntity> findCurrentByParticipant(@Param("userId") UUID userId);

  @Query(
      """
      select c.id from CallJpaEntity c
      where c.deadlineAt is not null and c.deadlineAt <= :now
      order by c.deadlineAt asc
      """)
  List<UUID> findDueIds(@Param("now") Instant now, Pageable pageable);

  @Query(
      value =
          """
          select r.call_id from call_runtime r
          join calls c on c.id = r.call_id
          where c.status in ('RINGING', 'CONNECTING', 'ACTIVE', 'RECOVERING')
            and (r.caller_lease_expires_at <= :now
              or (r.callee_lease_expires_at is not null and r.callee_lease_expires_at <= :now))
          order by least(r.caller_lease_expires_at,
            coalesce(r.callee_lease_expires_at, r.caller_lease_expires_at)) asc
          """,
      nativeQuery = true)
  List<UUID> findExpiredLeaseIds(@Param("now") Instant now, Pageable pageable);
}
