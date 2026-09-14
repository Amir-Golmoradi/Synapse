package dev.amir.synapse.identity.infrastructure.adapter.in.web.api;

import dev.amir.synapse.identity.domain.port.in.get_current_user.GetCurrentUserQuery;
import dev.amir.synapse.identity.domain.port.in.get_current_user.GetCurrentUserUseCase;
import dev.amir.synapse.identity.domain.port.in.user_search.UserSearchQuery;
import dev.amir.synapse.identity.domain.port.in.user_search.UserSearchUseCase;
import dev.amir.synapse.identity.domain.value_object.UserId;
import dev.amir.synapse.identity.infrastructure.adapter.in.web.dto.UserProfileResponse;
import dev.amir.synapse.identity.infrastructure.adapter.in.web.dto.UserSearchResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Authenticated user profile operations")
public class UserController {

  private final GetCurrentUserUseCase getCurrentUser;
  private final UserSearchUseCase userSearch;

  public UserController(GetCurrentUserUseCase getCurrentUser, UserSearchUseCase userSearch) {
    this.getCurrentUser = getCurrentUser;
    this.userSearch = userSearch;
  }

  @GetMapping("/me")
  @Operation(
      summary = "Get current user profile",
      description = "Returns the profile for the authenticated Synapse user.",
      security = @SecurityRequirement(name = "bearerAuth"),
      responses = {
        @ApiResponse(responseCode = "200", description = "Current user profile"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid access token"),
        @ApiResponse(responseCode = "404", description = "User not found")
      })
  public ResponseEntity<UserProfileResponse> me(Authentication authentication) {
    // UserId was set as principal by JwtAuthFilter
    var userId = UserId.of(authentication.getName());

    var result = getCurrentUser.handle(new GetCurrentUserQuery(userId));

    var response = UserProfileResponse.from(result);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/search")
  @Operation(
      summary = "Search users by Handle",
      description = "Returns a page of public user profiles whose Handle starts with the prefix.",
      security = @SecurityRequirement(name = "bearerAuth"),
      responses = {
        @ApiResponse(responseCode = "200", description = "Matching public user profiles"),
        @ApiResponse(responseCode = "400", description = "Invalid search parameters"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid access token")
      })
  public ResponseEntity<UserSearchResponse> search(
      @RequestParam String prefix,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    var result = userSearch.handle(new UserSearchQuery(prefix, page, size));
    return ResponseEntity.ok(UserSearchResponse.from(result));
  }
}
