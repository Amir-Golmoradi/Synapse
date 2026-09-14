package dev.amir.synapse.call.domain.exception;

import dev.amir.synapse.shared.domain.DomainException;

public class CallOperationException extends DomainException {
  private static final long serialVersionUID = 1L;
  private final String errorCode;
  private final String title;
  private final int httpStatus;

  public CallOperationException(String errorCode, String title, int httpStatus) {
    super(title);
    this.errorCode = errorCode;
    this.title = title;
    this.httpStatus = httpStatus;
  }

  public static CallOperationException conflict() {
    return new CallOperationException("CALL_STATE_CONFLICT", "Call state conflict", 409);
  }

  public static CallOperationException forbidden() {
    return new CallOperationException("CALL_ACTION_FORBIDDEN", "Call action forbidden", 403);
  }

  public static CallOperationException notFound() {
    return new CallOperationException("CALL_NOT_FOUND", "Call not found", 404);
  }

  public static CallOperationException busy() {
    return new CallOperationException("CALL_BUSY", "Participant is busy", 409);
  }

  public static CallOperationException clientNotReady() {
    return new CallOperationException("CALL_CLIENT_NOT_READY", "Call client is not ready", 409);
  }

  public static CallOperationException wrongClient() {
    return new CallOperationException("CALL_WRONG_CLIENT", "Call belongs to another client", 409);
  }

  public static CallOperationException staleGeneration() {
    return new CallOperationException("CALL_STALE_GENERATION", "Call negotiation is stale", 409);
  }

  public static CallOperationException idempotencyConflict() {
    return new CallOperationException(
        "CALL_IDEMPOTENCY_CONFLICT", "Call request identifier conflict", 409);
  }

  @Override
  public String getErrorCode() {
    return errorCode;
  }

  @Override
  public String getTitle() {
    return title;
  }

  @Override
  public int getHttpStatus() {
    return httpStatus;
  }
}
