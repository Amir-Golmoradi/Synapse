package dev.amir.synapse.messaging.infrastructure.adapter.in.web.rest;

import dev.amir.synapse.messaging.domain.exception.MessageRoomAccessDeniedException;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = MessageQueryApi.class)
class MessageQueryExceptionHandler {
  private static final URI INVALID_CURSOR_TYPE =
      URI.create("https://api.synapse.com/errors/message-cursor-invalid");
  private static final URI INVALID_REQUEST_TYPE =
      URI.create("https://api.synapse.com/errors/message-history-request-invalid");

  @ExceptionHandler(InvalidMessageCursorException.class)
  ResponseEntity<ProblemDetail> handleInvalidCursor(InvalidMessageCursorException exception) {
    return response(
        HttpStatus.BAD_REQUEST,
        INVALID_CURSOR_TYPE,
        "MESSAGE_CURSOR_INVALID",
        "Message cursor is invalid",
        "The message history cursor is invalid.");
  }

  @ExceptionHandler(MessageRoomAccessDeniedException.class)
  ResponseEntity<ProblemDetail> handleRoomNotFound(MessageRoomAccessDeniedException exception) {
    return response(
        HttpStatus.NOT_FOUND,
        exception.getTypeUri(),
        exception.getErrorCode(),
        exception.getTitle(),
        "The room was not found or is not accessible.");
  }

  @ExceptionHandler(ConstraintViolationException.class)
  ResponseEntity<ProblemDetail> handleInvalidRequest(ConstraintViolationException exception) {
    return response(
        HttpStatus.BAD_REQUEST,
        INVALID_REQUEST_TYPE,
        "MESSAGE_HISTORY_REQUEST_INVALID",
        "Message history request is invalid",
        "The message history request parameters are invalid.");
  }

  private static ResponseEntity<ProblemDetail> response(
      HttpStatus status, URI type, String errorCode, String title, String detail) {
    var problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setType(type);
    problem.setTitle(title);
    problem.setProperty("errorCode", errorCode);
    return ResponseEntity.status(status).body(problem);
  }
}
