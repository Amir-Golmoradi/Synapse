package dev.amir.synapse.identity.infrastructure.adapter.out.jwt;

import dev.amir.synapse.identity.application.port.out.access_token.CreateAccessTokenPort;
import dev.amir.synapse.identity.application.port.out.access_token.VerifyAccessTokenPort;
import dev.amir.synapse.identity.domain.value_object.UserId;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtAdapter implements CreateAccessTokenPort, VerifyAccessTokenPort {
  private final SecretKey signingKey;
  private final long tokenExpirationMs;

  public JwtAdapter(
      @Value("${synapse.jwt.secret}") String secretKey,
      @Value("${synapse.jwt.token-expiration-ms}") long tokenExpirationMs) {
    this.signingKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    this.tokenExpirationMs = tokenExpirationMs;
  }

  @Override
  public String createAccessToken(UserId userId) {
    return createToken(Map.of(), userId.getValue().toString());
  }

  @Override
  public Optional<UserId> verify(String accessToken) {
    try {
      if (isTokenExpired(accessToken)) {
        return Optional.empty();
      }
      return Optional.of(UserId.fromString(getSubject(accessToken)));
    } catch (JwtException | IllegalArgumentException ex) {
      return Optional.empty();
    }
  }

  private String getSubject(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  private String createToken(Map<String, Object> claims, String subject) {
    var now = Instant.now();
    return Jwts.builder()
        .claims(claims)
        .subject(subject)
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plus(tokenExpirationMs, ChronoUnit.MILLIS)))
        .signWith(signingKey)
        .compact();
  }

  private Claims getClaims(String token) {
    return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
  }

  private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    return claimsResolver.apply(getClaims(token));
  }

  private boolean isTokenExpired(String token) {
    return extractClaim(token, Claims::getExpiration).before(Date.from(Instant.now()));
  }
}
