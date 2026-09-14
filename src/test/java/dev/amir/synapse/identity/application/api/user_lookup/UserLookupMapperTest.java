package dev.amir.synapse.identity.application.api.user_lookup;

import static org.assertj.core.api.Assertions.assertThat;

import dev.amir.synapse.identity.domain.model.User;
import dev.amir.synapse.identity.domain.value_object.DisplayName;
import dev.amir.synapse.identity.domain.value_object.Email;
import dev.amir.synapse.identity.domain.value_object.Handle;
import dev.amir.synapse.identity.domain.value_object.UserId;
import org.junit.jupiter.api.Test;

class UserLookupMapperTest {

  @Test
  void exposesHandleAcrossTheIdentityContextBoundary() {
    var user =
        User.reconstitute(
            UserId.generate(),
            Email.of("private@example.com"),
            "private-google-subject",
            Handle.of("ami_r"),
            DisplayName.of("Amir"),
            null);

    var result = new UserLookupMapper().apply(user);

    assertThat(result.userId()).isEqualTo(user.getId().value());
    assertThat(result.handle()).isEqualTo("ami_r");
    assertThat(result.displayName()).isEqualTo("Amir");
  }
}
