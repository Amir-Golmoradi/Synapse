package dev.amir.synapse.call.infrastructure.adapter.out.persistence;

import dev.amir.synapse.call.application.port.out.CallReservationPort;
import dev.amir.synapse.call.domain.exception.CallOperationException;
import java.util.Comparator;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class CallReservationJdbcAdapter implements CallReservationPort {
  private final JdbcClient jdbcClient;

  CallReservationJdbcAdapter(JdbcClient jdbcClient) {
    this.jdbcClient = jdbcClient;
  }

  @Override
  public void reserve(UUID callId, UUID callerId, UUID calleeId) {
    try {
      var users =
          java.util.stream.Stream.of(callerId, calleeId).sorted(Comparator.naturalOrder()).toList();
      for (var user : users) {
        jdbcClient
            .sql("insert into call_user_reservations(user_id, call_id) values (:userId, :callId)")
            .param("userId", user)
            .param("callId", callId)
            .update();
      }
    } catch (DataIntegrityViolationException exception) {
      throw CallOperationException.busy();
    }
  }

  @Override
  public void release(UUID callId) {
    jdbcClient
        .sql("delete from call_user_reservations where call_id = :callId")
        .param("callId", callId)
        .update();
  }
}
