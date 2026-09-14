package dev.amir.synapse.identity.application.query.get_current_user;

import dev.amir.synapse.identity.application.port.out.user.LoadUserPort;
import dev.amir.synapse.identity.domain.exception.UserNotFoundException;
import dev.amir.synapse.identity.domain.port.in.get_current_user.GetCurrentUserQuery;
import dev.amir.synapse.identity.domain.port.in.get_current_user.GetCurrentUserResult;
import dev.amir.synapse.identity.domain.port.in.get_current_user.GetCurrentUserUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetCurrentUserHandler implements GetCurrentUserUseCase {

  private final LoadUserPort loadUser;

  public GetCurrentUserHandler(LoadUserPort loadUser) {
    this.loadUser = loadUser;
  }

  @Transactional
  @Override
  public GetCurrentUserResult handle(GetCurrentUserQuery query) {
    var user =
        loadUser
            .findById(query.userId())
            .orElseThrow(() -> new UserNotFoundException(query.userId()));

    return new GetCurrentUserResult(
        user.getId().getValue().toString(),
        user.getHandle().value(),
        user.getEmail(),
        user.getDisplayName().getValue(),
        user.getProfilePictureUrl());
  }
}
