package auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import java.text.ParseException;
import java.time.Clock;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.inject.Inject;

public final class IdTokens {
  @JsonProperty("idTokens")
  private Map<String, String> idTokens;

  private Clock clock;

  public IdTokens() {
    this.idTokens = new HashMap<>();
  }

  @JsonCreator
  @Inject
  public IdTokens(Clock clock, @Nullable @JsonProperty("idTokens") Map<String, String> idTokens) {
    this.clock = clock;
    this.idTokens = idTokens;
  }

  public Optional<String> getIdToken(String sessionId) {
    if (!idTokens.containsKey(sessionId)) {
      return Optional.empty();
    }
    return Optional.of(idTokens.get(sessionId));
  }

  public void storeIdToken(String sessionId, String idToken) {
    idTokens.put(sessionId, idToken);
  }

  public boolean removeIdToken(String sessionId) {
    return idTokens.remove(sessionId) != null;
  }

  public void purgeExpiredIdTokens() {
    idTokens
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
