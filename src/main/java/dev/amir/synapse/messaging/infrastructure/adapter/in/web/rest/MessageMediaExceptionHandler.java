package dev.amir.synapse.messaging.infrastructure.adapter.in.web.rest;

import dev.amir.synapse.messaging.domain.exception.MessageIdempotencyConflictException;
import dev.amir.synapse.messaging.domain.exception.MessageRoomAccessDeniedException;
import dev.amir.synapse.messaging.domain.exception.MessageValidationException;
import dev.amir.synapse.messaging.domain.exception.VideoMessageException;
import dev.amir.synapse.shared.domain.DomainException;
import java.net.URI;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice(assignableTypes = {VideoMessageCommandApi.class, MessageMediaQueryApi.class})
public class MessageMediaExceptionHandler {
  @ExceptionHandler({
    VideoMessageException.class,
    MessageRoomAccessDeniedException.class,
    MessageIdempotencyConflictException.class,
    MessageValidationException.class
  })
  ResponseEntity<ProblemDetail> handleDomain(DomainException exception) {
    var status = HttpStatus.valueOf(exception.getHttpStatus());
    var problem =
        ProblemDetail.forStatusAndDetail(status, sanitizedDetail(exception.getErrorCode()));
    problem.setTitle(exception.getTitle());
    problem.setType(errorType(exception.getErrorCode()));
    problem.setProperty("errorCode", exception.getErrorCode());
    return ResponseEntity.status(status).body(problem);
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  ResponseEntity<ProblemDetail> handleMaximumUpload(MaxUploadSizeExceededException exception) {
    return problem(
        HttpStatus.PAYLOAD_TOO_LARGE,
        "VIDEO_FILE_TOO_LARGE",
        "Video file is too large",
        "The video exceeds the size limit.");
  }

  @ExceptionHandler({
    MultipartException.class,
    MissingServletRequestParameterException.class,
    MissingServletRequestPartException.class
  })
  ResponseEntity<ProblemDetail> handleInvalidMultipart(Exception exception) {
    return problem(
        HttpStatus.BAD_REQUEST,
        "VIDEO_MESSAGE_REQUEST_INVALID",
        "Video message request is invalid",
        "The video message request is invalid.");
  }

  private static ResponseEntity<ProblemDetail> problem(
      HttpStatus status, String code, String title, String detail) {
    var problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setTitle(title);
    problem.setType(errorType(code));
    problem.setProperty("errorCode", code);
    return ResponseEntity.status(status).body(problem);
  }

  private static URI errorType(String errorCode) {
    return URI.create(
        "https://api.synapse.com/errors/" + errorCode.toLowerCase(Locale.ROOT).replace('_', '-'));
  }

  private static String sanitizedDetail(String errorCode) {
    return switch (errorCode) {
      case "MESSAGE_ROOM_NOT_FOUND" -> "The room was not found or is not accessible.";
      case "MESSAGE_MEDIA_NOT_FOUND" -> "The media was not found or is not accessible.";
      case "MESSAGE_IDEMPOTENCY_CONFLICT" ->
          "The client message ID has already been used for another message.";
      case "VIDEO_FILE_TOO_LARGE" -> "The video exceeds the size limit.";
      case "VIDEO_FORMAT_UNSUPPORTED" -> "The video format is unsupported.";
      case "VIDEO_DURATION_EXCEEDED" -> "The video exceeds the duration limit.";
      case "VIDEO_DIMENSIONS_EXCEEDED" -> "The video exceeds the dimension limit.";
      case "VIDEO_MEDIA_UNAVAILABLE" -> "Video media is temporarily unavailable.";
      default -> "The video message request is invalid.";
    };
  }
}
