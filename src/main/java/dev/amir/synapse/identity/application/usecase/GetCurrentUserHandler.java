package dev.amir.synapse.identity.application.usecase;

import dev.amir.synapse.identity.domain.exception.UserNotFoundException;
import dev.amir.synapse.identity.domain.port.in.get_current_user.GetCurrentUserQuery;
import dev.amir.synapse.identity.domain.port.in.get_current_user.GetCurrentUserResult;
import dev.amir.synapse.identity.domain.port.in.get_current_user.GetCurrentUserUseCase;
import dev.amir.synapse.identity.domain.port.out.user.LoadUserPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetCurrentUserHandler implements GetCurrentUserUseCase {

  private final LoadUserPort loadUser;

  public GetCurrentUserHandler(LoadUserPort loadUser) {
    this.loadUser = loadUser;
  }

  @Override
  public GetCurrentUserResult handle(GetCurrentUserQuery query) {
    var user =
        loadUser
            .findById(query.userId())
            .orElseThrow(() -> new UserNotFoundException(query.userId()));

    return new GetCurrentUserResult(
        user.getId().toString(),
        user.getEmail().getValue(),
        user.getFullName().getFirstName(),
        user.getFullName().getLastName(),
        user.getProfilePictureUrl());
  }
}
