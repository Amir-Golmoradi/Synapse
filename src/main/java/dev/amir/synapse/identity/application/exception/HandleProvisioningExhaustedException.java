package dev.amir.synapse.identity.application.exception;

import dev.amir.synapse.shared.domain.DomainException;
import java.io.Serial;

/** Initial Handle allocation could not complete despite its deterministic fallback. */
public final class HandleProvisioningExhaustedException extends DomainException {
  @Serial private static final long serialVersionUID = 1L;

  public HandleProvisioningExhaustedException() {
    super("The service could not allocate an initial Handle.");
  }

  public HandleProvisioningExhaustedException(Throwable cause) {
    super("The service could not allocate an initial Handle.", cause);
  }

  @Override
  public String getErrorCode() {
    return "IDENTITY_HANDLE_PROVISIONING_EXHAUSTED";
  }

  @Override
  public String getTitle() {
    return "Handle Provisioning Unavailable";
  }

  @Override
  public int getHttpStatus() {
    return 503;
  }
}
