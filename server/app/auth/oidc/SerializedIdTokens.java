package auth.oidc;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Class that manages the mapping between a jsonb db column and a Java Map. */
public final class SerializedIdTokens {
  private static final Logger LOGGER = LoggerFactory.getLogger(SerializedIdTokens.class);

  @JsonProperty("idTokens")
  private Map<String, String> idTokens;

  public SerializedIdTokens() {
    this.idTokens = new HashMap<>();
  }

  @JsonCreator
  public SerializedIdTokens(@Nullable @JsonProperty("idTokens") Map<String, String> idTokens) {
    if (idTokens == null) {
      this.idTokens = new HashMap<>();
    } else {
      this.idTokens = idTokens;
    }
  }

  /** Retrieves the ID token for the given sessionId, if present. */
  public Optional<String> getIdToken(String sessionId) {
    return Optional.ofNullable(idTokens.getOrDefault(sessionId, null));
  }

  /** Stores the ID token for the given sessionId. */
  public void storeIdToken(String sessionId, String idToken) {
    idTokens.put(sessionId, idToken);
  }

  /**
   * Remove the ID token for the given sessionId. Returns true if the specified sessionId was
   * present.
   */
  public boolean removeIdToken(String sessionId) {
    return idTokens.remove(sessionId) != null;
  }

  /**
   * Purges any ID tokens that have reached their expiration time. If an expiration time cannot be
   * found, the token is purged.
   */
  public void purgeExpiredIdTokens(Clock clock) {
    idTokens
        .entrySet()
        .removeIf(
            (entry) -> {
              try {
                JWT jwt = JWTParser.parse(entry.getValue());
                return isExpired(jwt, clock);
              } catch (ParseException e) {
                // Aggressively remove flawed tokens.
                LOGGER.warn("Purged ID token that could not be parsed as a JWT.");
                return true;
              }
            });
  }

  private static boolean isExpired(JWT jwt, Clock clock) {
    try {
      JWTClaimsSet claims = jwt.getJWTClaimsSet();
      Date expirationTime = claims.getExpirationTime();

      if (expirationTime == null) {
        // Aggressively remove flawed tokens.
        LOGGER.warn("Could not find expiration time claim in JWT; expiring anyway.");
        return true;
      }

      return expirationTime.toInstant().isBefore(clock.instant());
    } catch (ParseException e) {
      // Aggressively remove flawed tokens.
      LOGGER.warn("Could not parse JWT for claims to find expiration time; expiring anyway.");
      return true;
    }
  }
}
