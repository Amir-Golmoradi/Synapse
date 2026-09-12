package dev.amir.synapse.messaging.infrastructure.adapter.in.web.rest;

import dev.amir.synapse.shared.domain.DomainException;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice(assignableTypes = {VoiceMessageApi.class, VoiceMediaApi.class})
public class VoiceMessageExceptionHandler {
  private static final URI INVALID_REQUEST =
      URI.create("https://api.synapse.com/errors/voice-message-request-invalid");
  private static final URI TOO_LARGE =
      URI.create("https://api.synapse.com/errors/voice-message-too-large");

  @ExceptionHandler(DomainException.class)
  ResponseEntity<ProblemDetail> handleDomain(DomainException exception) {
    return response(
        HttpStatus.valueOf(exception.getHttpStatus()),
        exception.getTypeUri(),
        exception.getErrorCode(),
        exception.getTitle(),
        sanitizedDetail(exception));
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  ResponseEntity<ProblemDetail> handleTooLarge(MaxUploadSizeExceededException exception) {
    return response(
        HttpStatus.CONTENT_TOO_LARGE,
        TOO_LARGE,
        "VOICE_MESSAGE_TOO_LARGE",
        "Voice message is too large",
        "The voice message exceeds the maximum allowed size.");
  }

  @ExceptionHandler({
    MethodArgumentNotValidException.class,
    ConstraintViolationException.class,
    MissingServletRequestPartException.class,
    MultipartException.class
  })
  ResponseEntity<ProblemDetail> handleInvalidRequest(Exception exception) {
    return response(
        HttpStatus.BAD_REQUEST,
        INVALID_REQUEST,
        "VOICE_MESSAGE_REQUEST_INVALID",
        "Voice message request is invalid",
        "The voice message request is invalid.");
  }

  private static String sanitizedDetail(DomainException exception) {
    return switch (exception.getErrorCode()) {
      case "MESSAGE_ROOM_NOT_FOUND" -> "The room was not found or is not accessible.";
      case "MESSAGE_MEDIA_NOT_FOUND" -> "The message media was not found or is not accessible.";
      case "MESSAGE_IDEMPOTENCY_CONFLICT" ->
          "The client message ID has already been used for another message.";
      case "VOICE_MESSAGE_TOO_LARGE" -> "The voice message exceeds the maximum allowed size.";
      case "VOICE_MESSAGE_UNSUPPORTED_MEDIA" -> "The voice message media type is not supported.";
      case "VOICE_MEDIA_UNAVAILABLE" -> "The voice media is temporarily unavailable.";
      case "MESSAGE_MEDIA_RANGE_INVALID" ->
          "The requested media byte range is invalid or unsatisfiable.";
      default -> "The voice message request is invalid.";
    };
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
