package auth.oidc;

import static com.google.common.base.Preconditions.checkNotNull;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import java.text.ParseException;
import java.time.Clock;
import java.util.Date;
import java.util.Optional;

/** Class that manages stored ID tokens for an Account. */
public final class IdTokens {
  private SerializedIdTokens serializedIdTokens;

  private Clock clock;

  public IdTokens(Clock clock, SerializedIdTokens serializedIdTokens) {
    this.clock = checkNotNull(clock);
    this.serializedIdTokens = checkNotNull(serializedIdTokens);
  }

  public Optional<String> getIdToken(String sessionId) {
    if (!serializedIdTokens.containsKey(sessionId)) {
      return Optional.empty();
    }
    return Optional.of(serializedIdTokens.get(sessionId));
  }

  public void storeIdToken(String sessionId, String idToken) {
    serializedIdTokens.put(sessionId, idToken);
  }

  public boolean removeIdToken(String sessionId) {
    return serializedIdTokens.remove(sessionId) != null;
  }

  public void purgeExpiredIdTokens() {
    serializedIdTokens
        .entrySet()
        .removeIf(
            (entry) -> {
              try {
                JWT jwt = JWTParser.parse(entry.getValue());
                return isExpired(jwt);
              } catch (ParseException e) {
                // Aggressively remove flawed tokens.
                return true;
              }
            });
  }

  private boolean isExpired(JWT jwt) {
    try {
      JWTClaimsSet claims = jwt.getJWTClaimsSet();
      Date expirationTime = claims.getExpirationTime();
      if (expirationTime == null) {
        // Aggressively remove flawed tokens.
        return true;
      }
      return !clock.instant().isBefore(expirationTime.toInstant());
    } catch (ParseException e) {
      // Aggressively remove flawed tokens.
      return true;
    }
  }
}
