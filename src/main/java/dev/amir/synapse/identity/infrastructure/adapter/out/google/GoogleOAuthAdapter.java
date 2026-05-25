package dev.amir.synapse.identity.infrastructure.adapter.out.google;

import dev.amir.synapse.identity.domain.model.GoogleUserInfo;
import dev.amir.synapse.identity.domain.port.out.google.GoogleOAuthPort;
import dev.amir.synapse.identity.domain.value_object.Email;
import dev.amir.synapse.identity.domain.value_object.FullName;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class GoogleOAuthAdapter implements GoogleOAuthPort {

  private static final String TOKEN_INFO_URL =
      "https://oauth2.googleapis.com/tokeninfo?id_token={token}";

  private final RestClient restClient;

  public GoogleOAuthAdapter(RestClient.Builder builder) {
    this.restClient = builder.build();
  }

  @Override
  public GoogleUserInfo verifyIdToken(String idToken) {
    try {
      var response =
          restClient.get().uri(TOKEN_INFO_URL, idToken).retrieve().body(TokenInfoResponse.class);

      if (response == null) {
        throw new InvalidGoogleTokenException("Empty response from Google");
      }

      return new GoogleUserInfo(
          response.sub(),
          Email.of(response.email()),
          FullName.of(response.givenName(), response.familyName()),
          response.picture());

    } catch (InvalidGoogleTokenException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new InvalidGoogleTokenException("Google token verification failed", ex);
    }
  }
}
