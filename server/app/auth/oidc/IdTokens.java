package auth.oidc;

import static com.google.common.base.Preconditions.checkNotNull;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import java.text.ParseException;
import java.time.Clock;
import java.util.Date;
import java.util.Optional;
import models.AccountModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that manages stored OIDC ID tokens for an {@link AccountModel}.
 *
 * <p>ID tokens are JWT representations of user identities returned by an identity provider. We
 * store these because some identity providers require the current token to be supplied as part of a
 * logout request.
 *
 * <p>Since a single user (AccountModel) can be logged in simultaneously from different browsers, we
 * support storing multiple ID tokens, keyed by session id.
 */
public final class IdTokens {
  private static final Logger LOGGER = LoggerFactory.getLogger(IdTokens.class);

  private SerializedIdTokens serializedIdTokens;
  private Clock clock;

  public IdTokens(Clock clock, SerializedIdTokens serializedIdTokens) {
    this.clock = checkNotNull(clock);
    this.serializedIdTokens = checkNotNull(serializedIdTokens);
  }

  /** Retrieves the ID token for the given sessionId, if present. */
  public Optional<String> getIdToken(String sessionId) {
    return Optional.ofNullable(serializedIdTokens.getOrDefault(sessionId, null));
  }

  /** Stores the ID token for the given sessionId. */
  public void storeIdToken(String sessionId, String idToken) {
    serializedIdTokens.put(sessionId, idToken);
  }

  /**
   * Remove the ID token for the given sessionId. Returns true if the specified sessionId was
   * present.
   */
  public boolean removeIdToken(String sessionId) {
    return serializedIdTokens.remove(sessionId) != null;
  }

  /**
   * Purges any ID tokens that have reached their expiration time. If an expiration time cannot be
   * found, the token is purged.
   */
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
                LOGGER.warn("Purged ID token that could not be parsed as a JWT.");
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
