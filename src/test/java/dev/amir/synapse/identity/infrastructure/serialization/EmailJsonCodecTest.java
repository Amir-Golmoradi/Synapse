package dev.amir.synapse.identity.infrastructure.serialization;

import static org.assertj.core.api.Assertions.assertThat;

import dev.amir.synapse.identity.domain.port.in.get_current_user.GetCurrentUserResult;
import dev.amir.synapse.identity.domain.value_object.Email;
import dev.amir.synapse.identity.infrastructure.adapter.in.web.dto.UserProfileResponse;
import dev.amir.synapse.identity.infrastructure.adapter.out.oauth.google.TokenInfoResponse;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class EmailJsonCodecTest {

  private final JsonMapper jsonMapper = JsonMapper.builder().build();

  @Test
  void serializesProfileEmailAsScalarString() {
    var email = Email.of("amir@example.com");
    var result = new GetCurrentUserResult("user-123", "amir_gm", email, "Amir Gm", "avatar.png");
    var response = UserProfileResponse.from(result);

    var json = jsonMapper.writeValueAsString(response);

    assertThat(response.email()).isEqualTo(email);
    assertThat(json).contains("\"email\":\"amir@example.com\"");
    assertThat(json).doesNotContain("\"email\":{", "\"value\"");
  }

  @Test
  void deserializesGoogleEmailScalarToValueObject() {
    var response =
        jsonMapper.readValue("{\"email\":\"amir@example.com\"}", TokenInfoResponse.class);

    assertThat(response.email()).isEqualTo(Email.of("amir@example.com"));
  }

  @Test
  void deserializesBlankGoogleEmailAsNull() {
    var response = jsonMapper.readValue("{\"email\":\"  \"}", TokenInfoResponse.class);

    assertThat(response.email()).isNull();
  }
}
