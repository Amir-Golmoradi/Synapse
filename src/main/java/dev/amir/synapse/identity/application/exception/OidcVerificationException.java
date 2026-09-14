package dev.amir.synapse.identity.application.exception;

import dev.amir.synapse.shared.domain.DomainException;
import java.io.Serial;

/** The external identity token could not be verified into a trusted application profile. */
public final class OidcVerificationException extends DomainException {
  @Serial private static final long serialVersionUID = 1L;

  public OidcVerificationException(String message) {
    super(message);
  }

  public OidcVerificationException(String message, Throwable cause) {
    super(message, cause);
  }

  public static OidcVerificationException unsupportedProvider(String provider) {
    return new OidcVerificationException("Unsupported OIDC provider: " + provider);
  }

  @Override
  public String getErrorCode() {
    return "IDENTITY_OIDC_VERIFICATION_FAILED";
  }

  @Override
  public String getTitle() {
    return "OIDC Verification Failed";
  }

  @Override
  public int getHttpStatus() {
    return 401;
  }
}
