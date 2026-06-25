package dev.amir.synapse.identity.application.query.authenticate_access_token;

import dev.amir.synapse.identity.application.port.out.access_token.VerifyAccessTokenPort;
import dev.amir.synapse.identity.domain.port.in.access_token.AuthenticateAccessTokenQuery;
import dev.amir.synapse.identity.domain.port.in.access_token.AuthenticateAccessTokenUseCase;
import dev.amir.synapse.identity.domain.value_object.UserId;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class AuthenticateAccessTokenHandler implements AuthenticateAccessTokenUseCase {

  private final VerifyAccessTokenPort verifyAccessToken;

  public AuthenticateAccessTokenHandler(VerifyAccessTokenPort verifyAccessToken) {
    this.verifyAccessToken = verifyAccessToken;
  }

  @Override
  public Optional<UserId> handle(AuthenticateAccessTokenQuery query) {
    return verifyAccessToken.verify(query.accessToken());
  }
}
