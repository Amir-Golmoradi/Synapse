package dev.amir.synapse.identity.infrastructure.adapter.out.persistence.user;

import dev.amir.synapse.identity.application.exception.EmailConflictException;
import dev.amir.synapse.identity.application.exception.GoogleSubjectConflictException;
import dev.amir.synapse.identity.application.exception.HandleConflictException;
import dev.amir.synapse.identity.application.exception.UserIdConflictException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Optional;
import java.util.Set;
import org.hibernate.exception.ConstraintViolationException;
import org.postgresql.util.PSQLException;

final class UserConstraintViolationTranslator {
  private static final String HANDLE_CONSTRAINT = "uq_user_handle";
  private static final String GOOGLE_SUBJECT_CONSTRAINT = "uq_user_google_id";
  private static final String EMAIL_CONSTRAINT = "uq_user_email";
  private static final String USER_ID_CONSTRAINT = "pk_users";

  RuntimeException translate(RuntimeException exception) {
    return constraintName(exception).map(name -> translateKnown(name, exception)).orElse(exception);
  }

  static Optional<String> constraintName(Throwable exception) {
    String hibernateConstraint = null;
    String postgresConstraint = null;
    Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());

    for (var cause = exception; cause != null && visited.add(cause); cause = cause.getCause()) {
      if (cause instanceof PSQLException postgresException
          && postgresException.getServerErrorMessage() != null
          && postgresException.getServerErrorMessage().getConstraint() != null) {
        postgresConstraint = postgresException.getServerErrorMessage().getConstraint();
      }
      if (cause instanceof ConstraintViolationException hibernateException
          && hibernateException.getConstraintName() != null) {
        hibernateConstraint = hibernateException.getConstraintName();
      }
    }
    return Optional.ofNullable(
        postgresConstraint == null ? hibernateConstraint : postgresConstraint);
  }

  private static RuntimeException translateKnown(String name, RuntimeException cause) {
    return switch (name) {
      case HANDLE_CONSTRAINT -> new HandleConflictException(cause);
      case GOOGLE_SUBJECT_CONSTRAINT -> new GoogleSubjectConflictException(cause);
      case EMAIL_CONSTRAINT -> new EmailConflictException(cause);
      case USER_ID_CONSTRAINT -> new UserIdConflictException(cause);
      default -> cause;
    };
  }
}
