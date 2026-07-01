package dev.amir.synapse.messaging.infrastructure.adapter.in.web.rest;

import dev.amir.synapse.messaging.domain.exception.RoomOperationException;
import dev.amir.synapse.messaging.domain.exception.RoomValidationException;
import dev.amir.synapse.shared.domain.DomainException;
import java.net.URI;
import java.util.Locale;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {RoomCommandApi.class, RoomQueryApi.class})
class RoomExceptionHandler {
  private static final String ERROR_URI_BASE = "https://api.synapse.com/errors/";

  @ExceptionHandler({RoomValidationException.class, RoomOperationException.class})
  ResponseEntity<ProblemDetail> handleRoomException(DomainException exception) {
    var status = HttpStatus.valueOf(exception.getHttpStatus());
    var problem =
        problem(
            status,
            exception.getErrorCode(),
            exception.getTitle(),
            sanitizedDetail(exception.getErrorCode()));
    problem.setType(exception.getTypeUri());
    return ResponseEntity.status(status).body(problem);
  }

  @ExceptionHandler(SecurityException.class)
  ResponseEntity<ProblemDetail> handleSecurity(SecurityException exception) {
    return response(
        HttpStatus.FORBIDDEN,
        "ROOM_ACCESS_DENIED",
        "Room access denied",
        "You are not allowed to access this room.");
  }

  @ExceptionHandler(OptimisticLockingFailureException.class)
  ResponseEntity<ProblemDetail> handleOptimisticLock(OptimisticLockingFailureException exception) {
    return response(
        HttpStatus.CONFLICT,
        "ROOM_CONCURRENT_MODIFICATION",
        "Room changed concurrently",
        "The room was changed by another request. Reload and retry.");
  }

  private static ResponseEntity<ProblemDetail> response(
      HttpStatus status, String errorCode, String title, String detail) {
    return ResponseEntity.status(status).body(problem(status, errorCode, title, detail));
  }

  private static ProblemDetail problem(
      HttpStatus status, String errorCode, String title, String detail) {
    var problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setTitle(title);
    problem.setType(errorType(errorCode));
    problem.setProperty("errorCode", errorCode);
    return problem;
  }

  private static URI errorType(String errorCode) {
    return URI.create(ERROR_URI_BASE + errorCode.toLowerCase(Locale.ROOT).replace("_", "-"));
  }

  private static String sanitizedDetail(String errorCode) {
    return switch (errorCode) {
      case "DIRECT_MESSAGE_WITH_SELF" -> "A direct room requires two different users.";
      case "ROOM_PARTICIPANT_NOT_FOUND" ->
          "One or more requested room participants were not found.";
      case "ROOM_CONCURRENT_MODIFICATION" ->
          "The room was changed by another request. Reload and retry.";
      case "ROOM_VALIDATION_FAILED" -> "The room request violates a room rule.";
      default -> "The room request could not be completed.";
    };
  }
}
