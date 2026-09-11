package dev.amir.synapse.call.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.amir.synapse.call.application.model.CallRuntimeState;
import dev.amir.synapse.call.application.port.out.CallReservationPort;
import dev.amir.synapse.call.application.port.out.CallRuntimePort;
import dev.amir.synapse.call.domain.exception.CallOperationException;
import dev.amir.synapse.call.domain.model.Call;
import dev.amir.synapse.call.domain.port.out.LoadCallPort;
import dev.amir.synapse.call.domain.port.out.SaveCallPort;
import dev.amir.synapse.call.domain.value_object.CallParticipants;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    properties = {
      "server.port=0",
      "spring.jpa.hibernate.ddl-auto=validate",
      "spring.flyway.enabled=true",
      "spring.flyway.locations=classpath:db/create-table,classpath:db/alter-table",
      "spring.security.oauth2.client.registration.google.client-id=test-google-client-id",
      "spring.security.oauth2.client.registration.google.client-secret=test-google-client-secret",
      "synapse.google-token-url=http://localhost/tokeninfo?id_token={idToken}",
      "synapse.jwt.secret=01234567890123456789012345678901",
      "synapse.jwt.token-expiration-ms=900000",
      "synapse.call.timeout-sweep=1h"
    })
@Testcontainers
class CallPersistenceIntegrationTest {
  @Container
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("synapse_call_test")
          .withUsername("synapse")
          .withPassword("synapse");

  @Autowired private SaveCallPort saveCalls;
  @Autowired private LoadCallPort loadCalls;
  @Autowired private CallRuntimePort runtime;
  @Autowired private CallReservationPort reservations;

  @DynamicPropertySource
  static void datasource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @Test
  @Transactional
  void persistsCallRuntimeAndExclusiveParticipantReservations() {
    var caller = UUID.randomUUID();
    var callee = UUID.randomUUID();
    var now = Instant.parse("2026-09-10T10:00:00Z");
    var call =
        Call.start(
            new CallParticipants(caller, callee),
            UUID.randomUUID(),
            "fingerprint",
            now,
            now.plusSeconds(45));
    var saved = saveCalls.saveAndFlush(call);
    reservations.reserve(saved.getId().value(), caller, callee);
    runtime.save(
        new CallRuntimeState(
            saved.getId().value(),
            UUID.randomUUID(),
            null,
            0,
            false,
            false,
            false,
            false,
            now.plusSeconds(30),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            0,
            0,
            "test-instance"));

    assertThat(loadCalls.findCurrentByParticipant(caller)).isPresent();
    assertThat(runtime.findByCallId(saved.getId().value())).isPresent();

    var competing =
        Call.start(
            new CallParticipants(caller, UUID.randomUUID()),
            UUID.randomUUID(),
            "other",
            now,
            now.plusSeconds(45));
    var competingSaved = saveCalls.saveAndFlush(competing);
    assertThatThrownBy(
            () ->
                reservations.reserve(
                    competingSaved.getId().value(), caller, competing.participants().calleeId()))
        .isInstanceOf(CallOperationException.class)
        .extracting(exception -> ((CallOperationException) exception).getErrorCode())
        .isEqualTo("CALL_BUSY");
  }
}
