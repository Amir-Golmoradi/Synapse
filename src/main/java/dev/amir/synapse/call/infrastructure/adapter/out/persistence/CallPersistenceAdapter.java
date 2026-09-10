package dev.amir.synapse.call.infrastructure.adapter.out.persistence;

import dev.amir.synapse.call.application.model.CallRuntimeState;
import dev.amir.synapse.call.application.port.out.CallRuntimePort;
import dev.amir.synapse.call.application.port.out.FindDueCallsPort;
import dev.amir.synapse.call.domain.model.Call;
import dev.amir.synapse.call.domain.port.out.LoadCallPort;
import dev.amir.synapse.call.domain.port.out.SaveCallPort;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
class CallPersistenceAdapter
    implements LoadCallPort, SaveCallPort, CallRuntimePort, FindDueCallsPort {
  private final CallJpaRepository calls;
  private final CallRuntimeJpaRepository runtime;
  private final CallPersistenceMapper mapper;

  CallPersistenceAdapter(
      CallJpaRepository calls, CallRuntimeJpaRepository runtime, CallPersistenceMapper mapper) {
    this.calls = calls;
    this.runtime = runtime;
    this.mapper = mapper;
  }

  @Override
  public Optional<Call> findById(UUID id) {
    return calls.findById(id).map(mapper::toDomain);
  }

  @Override
  public Optional<Call> findByIdForUpdate(UUID id) {
    return calls.findByIdForUpdate(id).map(mapper::toDomain);
  }

  @Override
  public Optional<Call> findByCallerAndRequestId(UUID callerId, UUID requestId) {
    return calls.findByCallerIdAndClientRequestId(callerId, requestId).map(mapper::toDomain);
  }

  @Override
  public Optional<Call> findCurrentByParticipant(UUID userId) {
    return calls.findCurrentByParticipant(userId).map(mapper::toDomain);
  }

  @Override
  public Call save(Call call) {
    return mapper.toDomain(calls.save(mapper.toEntity(call)));
  }

  @Override
  public Call saveAndFlush(Call call) {
    return mapper.toDomain(calls.saveAndFlush(mapper.toEntity(call)));
  }

  @Override
  public Optional<CallRuntimeState> findByCallId(UUID callId) {
    return runtime.findById(callId).map(mapper::toDomain);
  }

  @Override
  public CallRuntimeState save(CallRuntimeState state) {
    return mapper.toDomain(runtime.save(mapper.toEntity(state)));
  }

  @Override
  public void delete(UUID callId) {
    runtime.deleteById(callId);
  }

  @Override
  public List<UUID> findDueCallIds(Instant now, int limit) {
    return calls.findDueIds(now, PageRequest.of(0, limit));
  }

  @Override
  public List<UUID> findExpiredLeaseCallIds(Instant now, int limit) {
    return calls.findExpiredLeaseIds(now, PageRequest.of(0, limit));
  }
}
