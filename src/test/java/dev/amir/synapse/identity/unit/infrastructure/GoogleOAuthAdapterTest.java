package dev.amir.synapse.identity.unit.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import dev.amir.synapse.identity.infrastructure.adapter.out.google.GoogleOAuthAdapter;
import dev.amir.synapse.identity.infrastructure.exception.InvalidGoogleTokenException;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GoogleOAuthAdapterTest {
  private static final String TOKEN_INFO_URL =
      "https://oauth2.googleapis.com/tokeninfo?id_token={token}";
  private static final String GOOGLE_CLIENT_ID = "synapse-google-client";

  private MockRestServiceServer googleServer;
  private GoogleOAuthAdapter adapter;

  @BeforeEach
  void setUp() {
    var builder = RestClient.builder();

    googleServer = MockRestServiceServer.bindTo(builder).build();

    var restClient = builder.build();

    adapter = new GoogleOAuthAdapter(restClient, TOKEN_INFO_URL, GOOGLE_CLIENT_ID);
  }

  @Test
  void verifyIdTokenReturnsGoogleUserInfoWhenTokenInfoIsValid() {
    var expiresAt = Instant.now().plusSeconds(300).getEpochSecond();
    googleServer
        .expect(once(), requestTo("https://oauth2.googleapis.com/tokeninfo?id_token=valid-token"))
        .andRespond(
            withSuccess(
                """
                {
                  "sub": "google-user-123",
                  "aud": "synapse-google-client",
                  "iss": "https://accounts.google.com",
                  "exp": %d,
                  "email": "user@example.com",
                  "given_name": "Amir",
                  "family_name": "Gm",
                  "email_verified": true,
                  "picture": "https://example.com/avatar.png"
                }
                """
                    .formatted(expiresAt),
                MediaType.APPLICATION_JSON));

    var userInfo = adapter.verifyIdToken("valid-token");

    assertThat(userInfo.googleId()).isEqualTo("google-user-123");
    assertThat(userInfo.email().getValue()).isEqualTo("user@example.com");
    assertThat(userInfo.fullName().getFirstName()).isEqualTo("Amir");
    assertThat(userInfo.fullName().getLastName()).isEqualTo("Gm");
    assertThat(userInfo.profilePictureUrl()).isEqualTo("https://example.com/avatar.png");
    googleServer.verify();
  }

  @Test
  void verifyIdTokenRejectsTokenForDifferentGoogleClient() {
    var expiresAt = Instant.now().plusSeconds(300).getEpochSecond();
    expectTokenInfo(
        "wrong-audience-token",
        """
        {
          "sub": "google-user-123",
          "aud": "another-client",
          "exp": %d,
          "email": "user@example.com",
          "given_name": "Amir",
          "family_name": "Gm",
          "email_verified": true,
          "picture": "https://example.com/avatar.png"
        }
        """
            .formatted(expiresAt));

    assertThatThrownBy(() -> adapter.verifyIdToken("wrong-audience-token"))
        .isInstanceOf(InvalidGoogleTokenException.class)
        .hasMessage("Google token audience mismatch");

    googleServer.verify();
  }

  @Test
  void verifyIdTokenRejectsMissingSubject() {
    var expiresAt = Instant.now().plusSeconds(300).getEpochSecond();
    expectTokenInfo(
        "missing-subject-token",
        """
        {
          "sub": " ",
          "aud": "synapse-google-client",
          "iss": "https://accounts.google.com",
          "exp": %d,
          "email": "user@example.com",
          "given_name": "Amir",
          "family_name": "Gm",
          "email_verified": true,
          "picture": "https://example.com/avatar.png"
        }
        """
            .formatted(expiresAt));

    assertThatThrownBy(() -> adapter.verifyIdToken("missing-subject-token"))
        .isInstanceOf(InvalidGoogleTokenException.class)
        .hasMessage("Google token subject is missing");

    googleServer.verify();
  }

  @Test
  void verifyIdTokenRejectsMissingEmail() {
    var expiresAt = Instant.now().plusSeconds(300).getEpochSecond();
    expectTokenInfo(
        "missing-email-token",
        """
        {
          "sub": "google-user-123",
          "aud": "synapse-google-client",
          "iss": "https://accounts.google.com",
          "exp": %d,
          "email": "",
          "given_name": "Amir",
          "family_name": "Gm",
          "email_verified": true,
          "picture": "https://example.com/avatar.png"
        }
        """
            .formatted(expiresAt));

    assertThatThrownBy(() -> adapter.verifyIdToken("missing-email-token"))
        .isInstanceOf(InvalidGoogleTokenException.class)
        .hasMessage("Google token email is missing");

    googleServer.verify();
  }

  @Test
  void verifyIdTokenRejectsMissingExpiration() {
    expectTokenInfo(
        "missing-expiration-token",
        """
        {
          "sub": "google-user-123",
          "aud": "synapse-google-client",
          "iss": "https://accounts.google.com",
          "email": "user@example.com",
          "given_name": "Amir",
          "family_name": "Gm",
          "email_verified": true,
          "picture": "https://example.com/avatar.png"
        }
        """);

    assertThatThrownBy(() -> adapter.verifyIdToken("missing-expiration-token"))
        .isInstanceOf(InvalidGoogleTokenException.class)
        .hasMessage("Google token has expired");

    googleServer.verify();
  }

  @Test
  void verifyIdTokenRejectsUnverifiedEmail() {
    var expiresAt = Instant.now().plusSeconds(300).getEpochSecond();
    expectTokenInfo(
        "unverified-email-token",
        """
        {
          "sub": "google-user-123",
          "aud": "synapse-google-client",
          "iss": "https://accounts.google.com",
          "exp": %d,
          "email": "user@example.com",
          "given_name": "Amir",
          "family_name": "Gm",
          "email_verified": false,
          "picture": "https://example.com/avatar.png"
        }
        """
            .formatted(expiresAt));

    assertThatThrownBy(() -> adapter.verifyIdToken("unverified-email-token"))
        .isInstanceOf(InvalidGoogleTokenException.class)
        .hasMessage("Google token email verification failed");

    googleServer.verify();
  }

  @Test
  void verifyIdTokenRejectsExpiredToken() {
    var expiredAt = Instant.now().minusSeconds(60).getEpochSecond();
    expectTokenInfo(
        "expired-token",
        """
        {
          "sub": "google-user-123",
          "aud": "synapse-google-client",
          "iss": "https://accounts.google.com",
          "exp": %d,
          "email": "user@example.com",
          "given_name": "Amir",
          "family_name": "Gm",
          "email_verified": true,
          "picture": "https://example.com/avatar.png"
        }
        """
            .formatted(expiredAt));

    assertThatThrownBy(() -> adapter.verifyIdToken("expired-token"))
        .isInstanceOf(InvalidGoogleTokenException.class)
        .hasMessage("Google token has expired");

    googleServer.verify();
  }

  @Test
  void verifyIdTokenRejectsEmptyGoogleResponse() {
    googleServer
        .expect(
            once(), requestTo("https://oauth2.googleapis.com/tokeninfo?id_token=empty-response"))
        .andRespond(withStatus(HttpStatus.NO_CONTENT));

    assertThatThrownBy(() -> adapter.verifyIdToken("empty-response"))
        .isInstanceOf(InvalidGoogleTokenException.class)
        .hasMessage("Google token response is empty");

    googleServer.verify();
  }

  @Test
  void verifyIdTokenAllowsMissingOptionalNameAndPictureClaims() {
    var expiresAt = Instant.now().plusSeconds(300).getEpochSecond();
    expectTokenInfo(
        "missing-optional-profile-claims",
        """
        {
          "sub": "google-user-123",
          "aud": "synapse-google-client",
          "iss": "https://accounts.google.com",
          "exp": %d,
          "email": "user@example.com",
          "email_verified": true
        }
        """
            .formatted(expiresAt));

    var userInfo = adapter.verifyIdToken("missing-optional-profile-claims");

    assertThat(userInfo.googleId()).isEqualTo("google-user-123");
    assertThat(userInfo.email().getValue()).isEqualTo("user@example.com");
    assertThat(userInfo.fullName().getFirstName()).isEmpty();
    assertThat(userInfo.fullName().getLastName()).isEmpty();
    assertThat(userInfo.profilePictureUrl()).isNull();
    googleServer.verify();
  }

  @Test
  void verifyIdTokenWrapsGoogleHttpFailure() {
    googleServer
        .expect(once(), requestTo("https://oauth2.googleapis.com/tokeninfo?id_token=google-down"))
        .andRespond(withServerError());

    assertThatThrownBy(() -> adapter.verifyIdToken("google-down"))
        .isInstanceOf(InvalidGoogleTokenException.class)
        .hasMessage("Google token verification failed")
        .hasCauseInstanceOf(Exception.class);

    googleServer.verify();
  }

  private void expectTokenInfo(String token, String body) {
    googleServer
        .expect(once(), requestTo("https://oauth2.googleapis.com/tokeninfo?id_token=" + token))
        .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
  }
}
