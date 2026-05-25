package dev.amir.synapse.shared.infrastructure.web;

import dev.amir.synapse.shared.domain.DomainException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

public abstract class BaseGlobalExceptionHandler extends ResponseEntityExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(BaseGlobalExceptionHandler.class);
  private static final String TRACE_ID = "traceId";
  private static final String TIMESTAMP = "timestamp";
  private static final String TRACE_HEADER = "X-Correlation-Id";

  @ExceptionHandler(Exception.class)
  public final ResponseEntity<ProblemDetail> handleAllExceptions(Exception ex, WebRequest request) {
    String traceId = resolveTraceId(request);
    return switch (ex) {
      case DomainException domainException -> handleDomainException(domainException, traceId);
      default -> handleGenericException(ex, traceId);
    };
  }

  private ResponseEntity<ProblemDetail> handleDomainException(
      DomainException domainEx, String traceId) {
    if (log.isWarnEnabled()) {
      log.warn(
          "[Trace: {}] Business Exception [{}]: {}",
          traceId,
          domainEx.getErrorCode(),
          domainEx.getMessage());
    }

    ProblemDetail problemDetail =
        ProblemDetail.forStatus(HttpStatusCode.valueOf(domainEx.getHttpStatus()));
    problemDetail.setType(domainEx.getTypeUri());
    problemDetail.setTitle(domainEx.getTitle());
    problemDetail.setDetail(domainEx.getMessage());

    // RFC 9457 Standard Extensions
    problemDetail.setProperty("code", domainEx.getErrorCode());
    problemDetail.setProperty(TIMESTAMP, Instant.now());
    problemDetail.setProperty(TRACE_ID, traceId);

    return ResponseEntity.status(problemDetail.getStatus()).body(problemDetail);
  }

  private ResponseEntity<ProblemDetail> handleGenericException(Exception ex, String traceId) {
    // Severe framework or connection anomalies are printed out with full operational stack traces
    log.error("[Trace: {}] Unhandled Infrastructure/System Exception: ", traceId, ex);

    ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
    problemDetail.setTitle("Internal Server Error");
    problemDetail.setDetail(
        "An unexpected error occurred on our systems. Please contact support if this persists.");

    problemDetail.setProperty("code", "INTERNAL_SERVER_ERROR");
    problemDetail.setProperty(TIMESTAMP, Instant.now());
    problemDetail.setProperty(TRACE_ID, traceId);

    return ResponseEntity.status(problemDetail.getStatus()).body(problemDetail);
  }

  @Override
  protected ResponseEntity<@NonNull Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      @NonNull HttpHeaders headers,
      @NonNull HttpStatusCode status,
      @NonNull WebRequest request) {

    String traceId = resolveTraceId(request);
    log.debug("[Trace: {}] Input validation failed for request parameters.", traceId);

    Map<String, String> validationErrors =
        ex.getBindingResult().getFieldErrors().stream()
            .collect(
                Collectors.toMap(
                    org.springframework.validation.FieldError::getField,
                    fieldError ->
                        fieldError.getDefaultMessage() != null
                            ? fieldError.getDefaultMessage()
                            : "Invalid input parameters provided",
                    (firstFound, duplicateFound) -> firstFound));

    ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
    problemDetail.setTitle("Validation Failed");
    problemDetail.setDetail("Payload constraints or structural field conditions were violated.");

    problemDetail.setProperty("code", "INVALID_INPUT_PAYLOAD");
    problemDetail.setProperty("errors", validationErrors);
    problemDetail.setProperty(TIMESTAMP, Instant.now());
    problemDetail.setProperty(TRACE_ID, traceId);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
  }

  private String resolveTraceId(WebRequest request) {
    String headerTrace = request.getHeader(TRACE_HEADER);
    return (headerTrace != null && !headerTrace.isBlank())
        ? headerTrace
        : UUID.randomUUID().toString();
  }
}
