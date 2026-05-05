package auth;

import com.google.common.annotations.VisibleForTesting;
import com.typesafe.config.Config;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.pac4j.core.context.FrameworkParameters;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.context.session.SessionStoreFactory;
import org.pac4j.play.store.DataEncrypter;
import org.pac4j.play.store.JdkAesDataEncrypter;
import org.pac4j.play.store.PlayCookieSessionStore;

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
    // This is a weird one.  :)  The cookie session store refuses to serialize any
    // classes it doesn't explicitly trust.  A bug in pac4j interacts badly with
    // sbt's autoreload, so we have a little workaround here.  configure() gets called on every
    // startup,
    // but the JAVA_SERIALIZER object is only initialized on initial startup.
    // So, on a second startup, we'll add the CiviFormProfileData a second time.  The
    // trusted classes set should dedupe CiviFormProfileData against the old CiviFormProfileData,
    // but it's technically a different class with the same name at that point,
    // which triggers the bug.  So, we just clear the classes, which will be empty
    // on first startup and will contain the profile on subsequent startups,
    // so that it's always safe to add the profile.
    // We will need to do this for every class we want to store in the cookie.
    var serializer = new org.pac4j.core.util.serializer.JavaSerializer();
    serializer.clearTrustedClasses();
    serializer.addTrustedClass(CiviFormProfileData.class);

    var sessionStore = new PlayCookieSessionStore(encrypter);
    sessionStore.setSerializer(serializer);
    return sessionStore;
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
