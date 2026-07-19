package dev.amir.synapse.identity.infrastructure.adapter.in.web.api;

import dev.amir.synapse.shared.domain.DomainException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(assignableTypes = {AuthController.class, UserController.class})
class IdentityExceptionHandler {
  private static final URI INVALID_REQUEST_TYPE =
      URI.create("https://api.synapse.com/errors/identity-invalid-request");

  @ExceptionHandler(DomainException.class)
  ResponseEntity<ProblemDetail> handleIdentityException(DomainException exception) {
    var status = HttpStatus.valueOf(exception.getHttpStatus());
    var problem =
        problem(
            status,
            exception.getTitle(),
            sanitizedDetail(exception.getErrorCode()),
            exception.getErrorCode(),
            exception.getTypeUri());
    return ResponseEntity.status(status).body(problem);
  }

  @ExceptionHandler({
    MethodArgumentNotValidException.class,
    HandlerMethodValidationException.class,
    MissingServletRequestParameterException.class,
    MethodArgumentTypeMismatchException.class,
    HttpMessageNotReadableException.class
  })
  ResponseEntity<ProblemDetail> handleInvalidRequest(Exception exception) {
    var status = HttpStatus.BAD_REQUEST;
    var problem =
        problem(
            status,
            "Invalid Identity Request",
            "The identity request parameters are invalid.",
            "IDENTITY_INVALID_REQUEST",
            INVALID_REQUEST_TYPE);
    return ResponseEntity.badRequest().body(problem);
  }

  private static ProblemDetail problem(
      HttpStatus status, String title, String detail, String errorCode, URI type) {
    var problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setTitle(title);
    problem.setType(type);
    problem.setProperty("errorCode", errorCode);
    return problem;
  }

  private static String sanitizedDetail(String errorCode) {
    return switch (errorCode) {
      case "IDENTITY_OIDC_VERIFICATION_FAILED" -> "The identity token could not be verified.";
      case "IDENTITY_ACCOUNT_CONFLICT" ->
          "The verified email is already associated with another account.";
      case "IDENTITY_HANDLE_PROVISIONING_EXHAUSTED" ->
          "A public Handle could not be allocated. Retry later.";
      case "IDENTITY_USER_NOT_FOUND" -> "The requested user was not found.";
      case "IDENTITY_INVALID_REFRESH_TOKEN" -> "The refresh token is invalid.";
      default -> "The identity request could not be completed.";
    };
  }
}
