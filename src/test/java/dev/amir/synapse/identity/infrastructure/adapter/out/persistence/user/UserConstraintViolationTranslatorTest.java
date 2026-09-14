package dev.amir.synapse.identity.infrastructure.adapter.out.persistence.user;

import static org.assertj.core.api.Assertions.assertThat;

import dev.amir.synapse.identity.application.exception.EmailConflictException;
import dev.amir.synapse.identity.application.exception.GoogleSubjectConflictException;
import dev.amir.synapse.identity.application.exception.HandleConflictException;
import dev.amir.synapse.identity.application.exception.UserIdConflictException;
import java.sql.SQLException;
import java.util.stream.Stream;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;

class UserConstraintViolationTranslatorTest {
  private final UserConstraintViolationTranslator translator =
      new UserConstraintViolationTranslator();

  @ParameterizedTest
  @MethodSource("knownConstraints")
  void translatesHibernateConstraintMetadata(
      String constraintName, Class<? extends RuntimeException> expectedType) {
    var violation =
        new ConstraintViolationException(
            "message deliberately names uq_user_unknown",
            new SQLException("message deliberately names uq_user_handle"),
            constraintName);
    var topLevel = new IllegalStateException("uq_user_email appears only in text", violation);

    var translated = translator.translate(topLevel);

    assertThat(translated).isInstanceOf(expectedType).hasCause(topLevel);
  }

  @Test
  void prefersPostgresConstraintMetadataAnywhereInCauseChain() {
    var postgres = postgresViolation("uq_user_google_id", "uq_user_handle appears only in text");
    var hibernate =
        new ConstraintViolationException(
            "Hibernate metadata points elsewhere", postgres, "uq_user_email");
    var topLevel = new IllegalStateException("outer wrapper", hibernate);

    var translated = translator.translate(topLevel);

    assertThat(translated).isInstanceOf(GoogleSubjectConflictException.class).hasCause(topLevel);
  }

  @Test
  void ignoresConstraintNamesThatAppearOnlyInExceptionMessages() {
    var exception =
        new IllegalStateException(
            "duplicate key violates uq_user_handle",
            new SQLException("constraint uq_user_email also appears here"));

    var translated = translator.translate(exception);

    assertThat(translated).isSameAs(exception);
    assertThat(UserConstraintViolationTranslator.constraintName(exception)).isEmpty();
  }

  @Test
  void leavesUnknownStructuredConstraintUntranslated() {
    var postgres = postgresViolation("uq_another_aggregate", "uq_user_handle");
    var exception = new IllegalStateException("persistence failure", postgres);

    var translated = translator.translate(exception);

    assertThat(translated).isSameAs(exception);
    assertThat(UserConstraintViolationTranslator.constraintName(exception))
        .contains("uq_another_aggregate");
  }

  private static PSQLException postgresViolation(String constraintName, String message) {
    var fields = "SERROR\0C23505\0M" + message + "\0n" + constraintName + "\0\0";
    return new PSQLException(new ServerErrorMessage(fields));
  }

  private static Stream<Arguments> knownConstraints() {
    return Stream.of(
        Arguments.of("uq_user_handle", HandleConflictException.class),
        Arguments.of("uq_user_google_id", GoogleSubjectConflictException.class),
        Arguments.of("uq_user_email", EmailConflictException.class),
        Arguments.of("pk_users", UserIdConflictException.class));
  }
}
