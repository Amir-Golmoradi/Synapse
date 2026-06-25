package dev.amir.synapse.identity.infrastructure.adapter.out.google;

import dev.amir.synapse.identity.application.port.out.google.GoogleOAuthPort;
import dev.amir.synapse.identity.domain.value_object.Email;
import dev.amir.synapse.identity.domain.value_object.FullName;
import dev.amir.synapse.identity.domain.value_object.GoogleUserInfo;
import dev.amir.synapse.identity.infrastructure.exception.InvalidGoogleTokenException;
import java.time.Clock;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class GoogleOAuthAdapter implements GoogleOAuthPort {

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
    this.validator = new GoogleTokenValidator(googleClientId, Clock.systemUTC());
  }

  @Override
  public GoogleUserInfo verifyIdToken(String idToken) {
    try {
      var tokenResponse =
          restClient.get().uri(tokenInfoUrl, idToken).retrieve().body(TokenInfoResponse.class);

      validator.validate(tokenResponse);

      return new GoogleUserInfo(
          Objects.requireNonNull(tokenResponse).sub(),
          Email.of(Objects.requireNonNull(tokenResponse.email())),
          FullName.of(tokenResponse.givenName(), tokenResponse.familyName()),
          tokenResponse.profilePicture());

    } catch (InvalidGoogleTokenException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new InvalidGoogleTokenException("Google token verification failed", ex);
    }
  }
}
