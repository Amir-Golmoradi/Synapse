package dev.amir.synapse.identity.infrastructure.adapter.in.web.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.amir.synapse.identity.application.exception.AccountConflictException;
import dev.amir.synapse.identity.application.exception.HandleProvisioningExhaustedException;
import dev.amir.synapse.identity.application.exception.OidcVerificationException;
import dev.amir.synapse.identity.domain.port.in.access_token.AuthenticateAccessTokenUseCase;
import dev.amir.synapse.identity.domain.port.in.google_signin.GoogleSignInCommand;
import dev.amir.synapse.identity.domain.port.in.google_signin.GoogleSignInResult;
import dev.amir.synapse.identity.domain.port.in.google_signin.GoogleSignInUseCase;
import dev.amir.synapse.identity.domain.port.in.logout.LogoutUseCase;
import dev.amir.synapse.identity.domain.port.in.refresh_token.RefreshTokenUseCase;
import dev.amir.synapse.shared.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerApiTest {
  @Autowired private MockMvc mockMvc;

  @MockitoBean private GoogleSignInUseCase googleSignIn;

  @MockitoBean private RefreshTokenUseCase refreshToken;

  @MockitoBean private LogoutUseCase logout;

  @MockitoBean private AuthenticateAccessTokenUseCase authenticateAccessToken;

  @Test
  void googleSignInResponseIncludesPublicHandle() throws Exception {
    when(googleSignIn.handle(any(GoogleSignInCommand.class)))
        .thenReturn(
            new GoogleSignInResult(
                "11111111-1111-1111-1111-111111111111",
                "amir_gm",
                "access-token",
                "refresh-token",
                "Amir",
                null));

    mockMvc
        .perform(
            post("/api/v1/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idToken\":\"verified-google-token\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").value("11111111-1111-1111-1111-111111111111"))
        .andExpect(jsonPath("$.handle").value("amir_gm"))
        .andExpect(jsonPath("$.displayName").value("Amir"));
  }

  @Test
  void malformedGoogleSignInReturnsIdentityInvalidInputProblem() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idToken\":\" \"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Invalid Identity Request"))
        .andExpect(jsonPath("$.errorCode").value("IDENTITY_INVALID_REQUEST"));

    verifyNoInteractions(googleSignIn);
  }

  @Test
  void oidcVerificationFailureReturnsSanitizedUnauthorizedProblem() throws Exception {
    when(googleSignIn.handle(any(GoogleSignInCommand.class)))
        .thenThrow(new OidcVerificationException("sensitive Google diagnostic"));

    mockMvc
        .perform(
            post("/api/v1/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idToken\":\"bad-token\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.detail").value("The identity token could not be verified."))
        .andExpect(jsonPath("$.errorCode").value("IDENTITY_OIDC_VERIFICATION_FAILED"));
  }

  @Test
  void verifiedEmailConflictReturnsConflictProblem() throws Exception {
    when(googleSignIn.handle(any(GoogleSignInCommand.class)))
        .thenThrow(new AccountConflictException());

    mockMvc
        .perform(
            post("/api/v1/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idToken\":\"valid-token\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errorCode").value("IDENTITY_ACCOUNT_CONFLICT"));
  }

  @Test
  void fallbackCollisionReturnsServiceUnavailableProblem() throws Exception {
    when(googleSignIn.handle(any(GoogleSignInCommand.class)))
        .thenThrow(new HandleProvisioningExhaustedException());

    mockMvc
        .perform(
            post("/api/v1/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idToken\":\"valid-token\"}"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.errorCode").value("IDENTITY_HANDLE_PROVISIONING_EXHAUSTED"));
  }
}
