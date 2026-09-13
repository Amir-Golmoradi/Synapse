package dev.amir.synapse.call.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.amir.synapse.call.application.model.CallRuntimeState;
import dev.amir.synapse.call.application.port.out.CallReservationPort;
import dev.amir.synapse.call.application.port.out.CallRuntimePort;
import dev.amir.synapse.call.domain.enums.CallMediaType;
import dev.amir.synapse.call.domain.exception.CallOperationException;
import dev.amir.synapse.call.domain.model.Call;
import dev.amir.synapse.call.domain.port.out.LoadCallPort;
import dev.amir.synapse.call.domain.port.out.SaveCallPort;
import dev.amir.synapse.call.domain.value_object.CallParticipants;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
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
  @Autowired private PlatformTransactionManager transactionManager;

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
            CallMediaType.VIDEO,
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
    assertThat(loadCalls.findById(saved.getId().value()).orElseThrow().mediaType())
        .isEqualTo(CallMediaType.VIDEO);
    assertThat(runtime.findByCallId(saved.getId().value())).isPresent();

    var competing =
        Call.start(
            new CallParticipants(caller, UUID.randomUUID()),
            CallMediaType.VOICE,
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

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void concurrentVoiceAndVideoStartsCannotReserveTheSameParticipant() throws Exception {
    var sharedUser = UUID.randomUUID();
    var now = Instant.parse("2026-09-10T10:00:00Z");
    var voice =
        saveCalls.saveAndFlush(
            Call.start(
                new CallParticipants(sharedUser, UUID.randomUUID()),
                CallMediaType.VOICE,
                UUID.randomUUID(),
                "voice",
                now,
                now.plusSeconds(45)));
    var video =
        saveCalls.saveAndFlush(
            Call.start(
                new CallParticipants(sharedUser, UUID.randomUUID()),
                CallMediaType.VIDEO,
                UUID.randomUUID(),
                "video",
                now,
                now.plusSeconds(45)));
    var ready = new CountDownLatch(2);
    var start = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(2)) {
      var voiceAttempt = executor.submit(() -> reserveAfterBarrier(voice, ready, start));
      var videoAttempt = executor.submit(() -> reserveAfterBarrier(video, ready, start));
      assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
      start.countDown();

      assertThat(java.util.List.of(voiceAttempt.get(), videoAttempt.get()))
          .containsExactlyInAnyOrder("RESERVED", "CALL_BUSY");
    }
  }

  private String reserveAfterBarrier(Call call, CountDownLatch ready, CountDownLatch start) {
    ready.countDown();
    try {
      if (!start.await(5, TimeUnit.SECONDS)) return "TIMEOUT";
      new TransactionTemplate(transactionManager)
          .executeWithoutResult(
              ignored ->
                  reservations.reserve(
                      call.getId().value(),
                      call.participants().callerId(),
                      call.participants().calleeId()));
      return "RESERVED";
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      return "INTERRUPTED";
    } catch (CallOperationException exception) {
      return exception.getErrorCode();
    }
  }
}
