package auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.typesafe.config.Config;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.pac4j.core.context.FrameworkParameters;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.context.session.SessionStoreFactory;
import org.pac4j.core.util.serializer.JsonSerializer;
import org.pac4j.play.store.DataEncrypter;
import org.pac4j.play.store.JdkAesDataEncrypter;
import org.pac4j.play.store.PlayCookieSessionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CiviFormSessionStoreFactory implements SessionStoreFactory {
  // Chosen based on OWASP's recommendation for PBKDF2-HMAC-SHA256
  private static final int PBKDF2_ITERATIONS = 600000;
  private static final int AES_KEY_BITS = 128;
  // This salt string should be a constant per "purpose", so that if we
  // ever want to reuse this technique in other parts of the app, we use
  // a different salt resulting in a different AES key.
  private static final byte[] SALT =
      "civiform:play-cookie-aes-key:v1".getBytes(StandardCharsets.UTF_8);
  private final DataEncrypter encrypter;

  /**
   * We need to use the secret key to generate the encrypter / decrypter AES key for the session
   * store in a way that always results in the same key, so that cookies generated from different
   * CiviForm server instances or different CiviForm versions are decryptable here.
   *
   * @param config Configuration object that contains the application secret
   */
  public CiviFormSessionStoreFactory(Config config) {
    byte[] aesKey;
    try {
      String secret = config.getString("play.http.secret.key");
      aesKey = deriveAes128Key(secret);
    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      // Should never really be possible given we hard code the algo/key spec
      throw new RuntimeException("Error generating cookie encryption key", e);
    }

    this.encrypter = new JdkAesDataEncrypter(aesKey);
  }

  @Override
  public SessionStore newSessionStore(FrameworkParameters parameters) {
    return newSessionStore();
  }

  public SessionStore newSessionStore() {
    var jsonSerializer = new JsonSerializer();
    // Play's request threads can have a context classloader that doesn't include the
    // application classes, so Jackson's DefaultTyping fails to resolve type ids like
    // "auth.CiviFormProfileData" with "no such class found". Bind the TypeFactory to the
    // classloader that actually loaded our app classes.
    ObjectMapper jsonMapper = jsonSerializer.getObjectMapper();
    jsonMapper.setTypeFactory(
        jsonMapper.getTypeFactory().withClassLoader(CiviFormProfileData.class.getClassLoader()));

    var sessionStore = new InvalidatingCookieSessionStore(encrypter);
    sessionStore.setSerializer(jsonSerializer);
    return sessionStore;
  }

  /**
   * A {@link PlayCookieSessionStore} that treats an unreadable session cookie as no session rather
   * than an error.
   *
   * <p>A cookie can be unreadable because it was written by a CiviForm version predating the
   * play-pac4j 13 upgrade (Shiro encryption / Java serialization, previously handled by fallback
   * decrypters and serializers that have since been removed), or because the application secret
   * changed. In either case the right outcome is to invalidate the session and treat the user as
   * logged out, not to fail the request.
   */
  private static final class InvalidatingCookieSessionStore extends PlayCookieSessionStore {
    private static final Logger logger =
        LoggerFactory.getLogger(InvalidatingCookieSessionStore.class);

    InvalidatingCookieSessionStore(DataEncrypter dataEncrypter) {
      super(dataEncrypter);
    }

    @Override
    protected Map<String, Object> getSessionValues(WebContext context) {
      try {
        return super.getSessionValues(context);
      } catch (RuntimeException e) {
        logger.warn("Session cookie could not be read, invalidating the session", e);
        // Drop the unreadable cookie so we don't re-attempt (and re-log) on every request.
        putSessionValues(context, null);
        return new HashMap<>();
      }
    }
  }

  /**
   * Derives a 16-byte AES key utilizing PBKDF2-HMAC-SHA256. This algorithm is designed for deriving
   * keys from secrets that might be human-chosen, low entropy, or otherwise guessable. The Play app
   * secret should be randomly generated, but this is not absolutely enforced and someone could
   * choose to use some insecure string instead.
   *
   * <p>To mitigate this, PBKDF2 runs the HMAC operation PBKDF2_ITERATIONS times while generating
   * the 128-bit AES key. This makes brute forcing much slower than, say, taking a truncated version
   * of a SHA-256 digest of the app secret. Additionally, by utilizing a salt, if we use the app
   * secret to derive keys in other parts of the application in a similar manner, the resulting keys
   * will be different.
   *
   * <p>With play-pac4j 13.x, we have no choice but to use a 16-byte key. But AES-128 should be
   * sufficiently secure for this purpose with this method of derivation.
   *
   * @param playSecret The Play application secret string, or other sufficiently random string.
   * @return A 16-byte AES key
   */
  @VisibleForTesting
  static byte[] deriveAes128Key(String playSecret)
      throws NoSuchAlgorithmException, InvalidKeySpecException {
    PBEKeySpec spec =
        new PBEKeySpec(
            /* password= */ playSecret.toCharArray(),
            /* salt= */ SALT,
            /* iterationCount= */ PBKDF2_ITERATIONS,
            /* keyLength= */ AES_KEY_BITS);

    SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
    // Clear the secret out of memory used here regardless of
    // if this works
    try {
      return skf.generateSecret(spec).getEncoded();
    } finally {
      spec.clearPassword();
    }
  }
}
