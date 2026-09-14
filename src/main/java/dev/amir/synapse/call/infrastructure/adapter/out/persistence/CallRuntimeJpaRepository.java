package dev.amir.synapse.call.infrastructure.adapter.out.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface CallRuntimeJpaRepository extends JpaRepository<CallRuntimeJpaEntity, UUID> {}
