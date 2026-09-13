package dev.amir.synapse.call.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class CallMediaTypeMigrationIntegrationTest {
  @Container
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("synapse_call_migration_test")
          .withUsername("synapse")
          .withPassword("synapse");

  @Test
  void backfillsExistingCallsAsVoiceAndConstrainsFutureValues() throws SQLException {
    migrateTo("7");
    var callId = UUID.randomUUID();
    try (var connection =
            DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        var statement = connection.createStatement()) {
      statement.executeUpdate(
          """
          INSERT INTO calls (
              id, caller_id, callee_id, client_request_id, start_request_fingerprint,
              status, created_at, updated_at, deadline_at, version
          ) VALUES (
              '%s', '%s', '%s', '%s', '%s',
              'RINGING', now(), now(), now() + interval '45 seconds', 0
          )
          """
              .formatted(
                  callId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "a".repeat(64)));
    }

    migrateTo("8");

    try (var connection =
            DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        var statement = connection.createStatement();
        var result =
            statement.executeQuery("SELECT media_type FROM calls WHERE id = '" + callId + "'")) {
      assertThat(result.next()).isTrue();
      assertThat(result.getString(1)).isEqualTo("VOICE");
    }
    try (var connection =
            DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        var statement = connection.createStatement()) {
      assertThatThrownBy(
              () ->
                  statement.executeUpdate(
                      "UPDATE calls SET media_type = 'SCREEN' WHERE id = '" + callId + "'"))
          .isInstanceOf(SQLException.class);
    }
  }

  private void migrateTo(String version) {
    Flyway.configure()
        .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
        .locations("classpath:db/create-table", "classpath:db/alter-table")
        .target(version)
        .load()
        .migrate();
  }
}
