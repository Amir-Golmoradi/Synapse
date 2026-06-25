package dev.amir.synapse.identity.domain.port.in.get_current_user;

@FunctionalInterface
public interface GetCurrentUserUseCase {
  GetCurrentUserResult handle(GetCurrentUserQuery query);
}
