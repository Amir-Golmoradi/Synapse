package dev.amir.synapse.identity.domain.port.in.google_signin;

@FunctionalInterface
public interface GoogleSignInUseCase {

  GoogleSignInResult handle(GoogleSignInCommand googleSignInCommand);
}
