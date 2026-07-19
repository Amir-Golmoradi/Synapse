package dev.amir.synapse.identity.application.port.out.oauth;

@FunctionalInterface
public interface OidcPort {
  VerifiedOidcProfile verifyIdToken(String idToken);
}
