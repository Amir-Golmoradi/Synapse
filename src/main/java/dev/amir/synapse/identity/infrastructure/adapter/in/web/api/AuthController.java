package dev.amir.synapse.identity.infrastructure.adapter.in.web.api;

import dev.amir.synapse.identity.domain.port.in.google_signin.GoogleSignInCommand;
import dev.amir.synapse.identity.domain.port.in.google_signin.GoogleSignInUseCase;
import dev.amir.synapse.identity.domain.port.in.logout.LogoutCommand;
import dev.amir.synapse.identity.domain.port.in.logout.LogoutUseCase;
import dev.amir.synapse.identity.domain.port.in.refresh_token.RefreshTokenCommand;
import dev.amir.synapse.identity.domain.port.in.refresh_token.RefreshTokenUseCase;
import dev.amir.synapse.identity.infrastructure.adapter.in.web.dto.GoogleSignInRequest;
import dev.amir.synapse.identity.infrastructure.adapter.in.web.dto.GoogleSignInResponse;
import dev.amir.synapse.identity.infrastructure.adapter.in.web.dto.LogoutRequest;
import dev.amir.synapse.identity.infrastructure.adapter.in.web.dto.RefreshRequest;
import dev.amir.synapse.identity.infrastructure.adapter.in.web.dto.RefreshResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Google sign-in, token refresh, and logout")
public class AuthController {

  private final GoogleSignInUseCase googleSignIn;
  private final RefreshTokenUseCase refreshToken;
  private final LogoutUseCase logoutUseCase;

  public AuthController(
      GoogleSignInUseCase googleSignIn, RefreshTokenUseCase refreshToken, LogoutUseCase logout) {
    this.googleSignIn = googleSignIn;
    this.refreshToken = refreshToken;
    this.logoutUseCase = logout;
  }

  @PostMapping("/google")
  @Operation(
      summary = "Sign in with Google",
      description = "Exchanges a Google ID token for Synapse access and refresh tokens.",
      responses = {
        @ApiResponse(responseCode = "200", description = "Sign-in completed"),
        @ApiResponse(responseCode = "400", description = "Malformed request"),
        @ApiResponse(responseCode = "401", description = "Invalid Google token")
      })
  public ResponseEntity<GoogleSignInResponse> signInWithGoogle(
      @Valid @RequestBody GoogleSignInRequest request) {

    var signInCommand = new GoogleSignInCommand(request.idToken());
    var result = googleSignIn.handle(signInCommand);

    var response = GoogleSignInResponse.from(result);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/refresh")
  @Operation(
      summary = "Refresh tokens",
      description = "Rotates a refresh token and returns a new access token pair.",
      responses = {
        @ApiResponse(responseCode = "200", description = "Token pair refreshed"),
        @ApiResponse(responseCode = "400", description = "Malformed request"),
        @ApiResponse(responseCode = "401", description = "Invalid refresh token")
      })
  public ResponseEntity<RefreshResponse> refresh(@Valid @RequestBody RefreshRequest request) {

    var result = refreshToken.handle(new RefreshTokenCommand(request.refreshToken()));

    return ResponseEntity.ok(new RefreshResponse(result.accessToken(), result.refreshToken()));
  }

  @PostMapping("/logout")
  @Operation(
      summary = "Log out",
      description = "Revokes the supplied refresh token.",
      responses = {
        @ApiResponse(responseCode = "204", description = "Refresh token revoked"),
        @ApiResponse(responseCode = "400", description = "Malformed request"),
        @ApiResponse(responseCode = "401", description = "Invalid refresh token")
      })
  public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
    var command = new LogoutCommand(request.refreshToken());
    logoutUseCase.handle(command);
    return ResponseEntity.noContent().build();
  }
}
