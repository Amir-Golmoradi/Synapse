package dev.amir.synapse.call.infrastructure.adapter.in.web.rest;

import dev.amir.synapse.call.domain.exception.CallOperationException;
import dev.amir.synapse.call.domain.exception.CallValidationException;
import dev.amir.synapse.shared.domain.DomainException;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {CallCommandApi.class, CallQueryApi.class})
public class CallExceptionHandler {
  @ExceptionHandler({CallOperationException.class, CallValidationException.class})
  ResponseEntity<ProblemDetail> handleDomain(DomainException exception) {
    var status = HttpStatus.valueOf(exception.getHttpStatus());
    var detail =
        switch (exception.getErrorCode()) {
          case "CALL_NOT_FOUND" -> "The call was not found or is not accessible.";
          case "CALL_BUSY" -> "One of the call participants is already busy.";
          case "CALL_CLIENT_NOT_READY" -> "Connect and register the call client before retrying.";
          case "CALL_WRONG_CLIENT" -> "The call is owned by another browser session.";
          case "CALL_STALE_GENERATION" -> "Refresh the call state before retrying.";
          default -> "The call request could not be completed.";
        };
    var problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setTitle(exception.getTitle());
    problem.setType(exception.getTypeUri());
    problem.setProperty("errorCode", exception.getErrorCode());
    return ResponseEntity.status(status).body(problem);
  }

  @ExceptionHandler({
    MethodArgumentNotValidException.class,
    HttpMessageNotReadableException.class,
    ConstraintViolationException.class
  })
  ResponseEntity<ProblemDetail> handleInvalidRequest(Exception exception) {
    var problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "The call request is invalid.");
    problem.setTitle("Call request is invalid");
    problem.setType(URI.create("https://api.synapse.com/errors/call-request-invalid"));
    problem.setProperty("errorCode", "CALL_REQUEST_INVALID");
    return ResponseEntity.badRequest().body(problem);
  }
}
