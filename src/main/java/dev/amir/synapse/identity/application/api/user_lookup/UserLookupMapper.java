package dev.amir.synapse.identity.application.api.user_lookup;

import dev.amir.synapse.identity.domain.model.User;
import java.util.function.Function;
import org.springframework.stereotype.Component;

@Component
public class UserLookupMapper implements Function<User, UserLookupResult> {
  @Override
  public UserLookupResult apply(User user) {
    return new UserLookupResult(
        user.getId().getValue(), user.getFullName().toString(), user.getProfilePictureUrl());
  }
}
