package dev.amir.synapse.identity.domain.port.in.logout;

@FunctionalInterface
public interface LogoutUseCase {
  void handle(LogoutCommand command);
}
