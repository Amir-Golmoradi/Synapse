package dev.amir.synapse.identity.infrastructure.adapter.out.oauth.google;

import dev.amir.synapse.identity.application.exception.OidcVerificationException;
import dev.amir.synapse.identity.application.port.out.oauth.OidcPort;
import dev.amir.synapse.identity.application.port.out.oauth.VerifiedOidcProfile;
import dev.amir.synapse.identity.domain.value_object.DisplayName;
import java.time.Clock;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class GoogleOAuthAdapter implements OidcPort {
  static final String PROVIDER = "google";
  private static final String MISSING_NAME = "Google User";

  private final String tokenInfoUrl;
  private final RestClient restClient;
  private final GoogleTokenValidator validator;

  public GoogleOAuthAdapter(
      @Qualifier("googleRestClient") RestClient restClient,
      @Value("${synapse.google-token-url}") String tokenInfoUrl,
      @Value("${spring.security.oauth2.client.registration.google.client-id}")
          String googleClientId) {
    this.tokenInfoUrl = tokenInfoUrl;
    this.restClient = restClient;
    validator = new GoogleTokenValidator(googleClientId, Clock.systemUTC());
  }

  @Override
  public VerifiedOidcProfile verifyIdToken(String idToken) {
    try {
      var response =
          restClient.get().uri(tokenInfoUrl, idToken).retrieve().body(TokenInfoResponse.class);
      validator.validate(response);
      var verified = Objects.requireNonNull(response);

      return new VerifiedOidcProfile(
          PROVIDER,
          Objects.requireNonNull(verified.sub()),
          Objects.requireNonNull(verified.email()),
          DisplayName.of(displayNameFrom(verified)),
          verified.profilePicture());
    } catch (GoogleTokenVerificationException exception) {
      throw new OidcVerificationException(exception.getMessage(), exception);
    } catch (OidcVerificationException exception) {
      throw exception;
    } catch (RuntimeException exception) {
      throw new OidcVerificationException("Google token verification failed", exception);
    }
  }

  private static String displayNameFrom(TokenInfoResponse response) {
    if (response.name() != null && !response.name().isBlank()) {
      return response.name();
    }
    var givenName = response.givenName() == null ? "" : response.givenName().strip();
    var familyName = response.familyName() == null ? "" : response.familyName().strip();
    var combined = (givenName + " " + familyName).strip();
    return combined.isEmpty() ? MISSING_NAME : combined;
  }
}
