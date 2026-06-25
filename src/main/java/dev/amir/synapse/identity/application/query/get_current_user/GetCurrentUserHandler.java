package dev.amir.synapse.identity.application.query.get_current_user;

import dev.amir.synapse.identity.application.port.out.user.LoadUserPort;
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
    var user = loadUser.findById(query.userId()).orElseThrow();

    return new GetCurrentUserResult(
        user.getId().getValue().toString(),
        user.getEmail().getValue(),
        user.getFullName().getFirstName(),
        user.getFullName().getLastName(),
        user.getProfilePictureUrl());
  }
}
