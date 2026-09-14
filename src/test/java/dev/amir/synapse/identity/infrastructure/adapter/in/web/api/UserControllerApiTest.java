package dev.amir.synapse.identity.infrastructure.adapter.in.web.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.amir.synapse.identity.domain.port.in.access_token.AuthenticateAccessTokenUseCase;
import dev.amir.synapse.identity.domain.port.in.get_current_user.GetCurrentUserQuery;
import dev.amir.synapse.identity.domain.port.in.get_current_user.GetCurrentUserResult;
import dev.amir.synapse.identity.domain.port.in.get_current_user.GetCurrentUserUseCase;
import dev.amir.synapse.identity.domain.port.in.user_search.UserSearchItem;
import dev.amir.synapse.identity.domain.port.in.user_search.UserSearchQuery;
import dev.amir.synapse.identity.domain.port.in.user_search.UserSearchResult;
import dev.amir.synapse.identity.domain.port.in.user_search.UserSearchUseCase;
import dev.amir.synapse.identity.domain.value_object.Email;
import dev.amir.synapse.shared.config.SecurityConfig;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerApiTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private GetCurrentUserUseCase getCurrentUserUseCase;

  @MockitoBean private UserSearchUseCase userSearchUseCase;

  @MockitoBean private AuthenticateAccessTokenUseCase authenticateAccessTokenUseCase;

  @Test
  @WithMockUser
  void searchNormalizesPrefixUsesDefaultsAndReturnsOnlyPublicIdentityFields() throws Exception {
    var userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    when(userSearchUseCase.handle(any(UserSearchQuery.class)))
        .thenReturn(
            new UserSearchResult(
                List.of(new UserSearchItem(userId, "ami._1", "Amir", null)), 0, 20, true));

    mockMvc
        .perform(get("/api/v1/users/search").param("prefix", "AmI._"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].userId").value(userId.toString()))
        .andExpect(jsonPath("$.items[0].handle").value("ami._1"))
        .andExpect(jsonPath("$.items[0].displayName").value("Amir"))
        .andExpect(jsonPath("$.items[0].profilePictureUrl").value(nullValue()))
        .andExpect(jsonPath("$.items[0].email").doesNotExist())
        .andExpect(jsonPath("$.items[0].googleId").doesNotExist())
        .andExpect(jsonPath("$.items[0].subjectId").doesNotExist())
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(20))
        .andExpect(jsonPath("$.hasNext").value(true));

    var queryCaptor = ArgumentCaptor.forClass(UserSearchQuery.class);
    verify(userSearchUseCase).handle(queryCaptor.capture());
    assertThat(queryCaptor.getValue())
        .satisfies(
            query -> {
              assertThat(query.prefix()).isEqualTo("ami._");
              assertThat(query.page()).isZero();
              assertThat(query.size()).isEqualTo(20);
            });
  }

  @Test
  @WithMockUser
  void invalidPrefixReturnsIdentityProblemDetailWithoutCallingUseCase() throws Exception {
    mockMvc
        .perform(get("/api/v1/users/search").param("prefix", "ami..r"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Invalid Identity Request"))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.errorCode").value("IDENTITY_INVALID_REQUEST"));

    verifyNoInteractions(userSearchUseCase);
  }

  @Test
  @WithMockUser(username = "11111111-1111-1111-1111-111111111111")
  void currentProfileIncludesPublicHandle() throws Exception {
    when(getCurrentUserUseCase.handle(any(GetCurrentUserQuery.class)))
        .thenReturn(
            new GetCurrentUserResult(
                "11111111-1111-1111-1111-111111111111",
                "ami_r",
                Email.of("private@example.com"),
                "Amir",
                null));

    mockMvc
        .perform(get("/api/v1/users/me"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").value("11111111-1111-1111-1111-111111111111"))
        .andExpect(jsonPath("$.handle").value("ami_r"))
        .andExpect(jsonPath("$.email").value("private@example.com"))
        .andExpect(jsonPath("$.displayName").value("Amir"));
  }

  @Test
  void searchRequiresAuthentication() throws Exception {
    mockMvc
        .perform(get("/api/v1/users/search").param("prefix", "ami"))
        .andExpect(status().isUnauthorized());

    verifyNoInteractions(userSearchUseCase);
  }
}
