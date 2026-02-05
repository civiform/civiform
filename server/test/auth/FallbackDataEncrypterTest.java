package auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import org.apache.shiro.crypto.AesCipherService;
import org.junit.Test;
import org.pac4j.play.store.JdkAesDataEncrypter;

public class FallbackDataEncrypterTest {
  private static final String APP_SECRET = "app-secret";
  private static final byte[] NEW_KEY = deriveNewKey(APP_SECRET);
  private static final byte[] LEGACY_KEY =
      CiviFormSessionStoreFactory.deriveLegacyAesKey(APP_SECRET);
  private static final JdkAesDataEncrypter PRIMARY_ENCRYPTER = new JdkAesDataEncrypter(NEW_KEY);
  private static final ShiroAesDataEncrypter FALLBACK_ENCRYPTER =
      new ShiroAesDataEncrypter(LEGACY_KEY);
  private static final FallbackDataEncrypter COMBINED_ENCRYPTER =
      new FallbackDataEncrypter(PRIMARY_ENCRYPTER, FALLBACK_ENCRYPTER);

  /** Wraps the checked exception for use in static initialization. */
  private static byte[] deriveNewKey(String secret) {
    try {
      return CiviFormSessionStoreFactory.deriveAes128Key(secret);
    } catch (Exception e) {
      throw new RuntimeException("Failed to derive test key", e);
    }
  }

  /**
   * Encrypts using Shiro cipher to simulate old session cookies. We use this helper because
   * ShiroAesDataEncrypter intentionally disables encryption to prevent accidental use.
   */
  private static byte[] shiroEncrypt(byte[] plaintext, byte[] key) {
    return new AesCipherService().encrypt(plaintext, key).getBytes();
  }

  @Test
  public void encrypt_usesPrimaryEncrypter() {
    byte[] plaintext = "test data".getBytes(StandardCharsets.UTF_8);
    byte[] encrypted = COMBINED_ENCRYPTER.encrypt(plaintext);

    assertThat(encrypted).isNotEqualTo(plaintext);
    assertThat(PRIMARY_ENCRYPTER.decrypt(encrypted)).isEqualTo(plaintext);
  }

  @Test
  public void decrypt_usesPrimaryForNewEncryption() {
    byte[] plaintext = "test data for primary".getBytes(StandardCharsets.UTF_8);
    byte[] encrypted = PRIMARY_ENCRYPTER.encrypt(plaintext);

    assertThat(COMBINED_ENCRYPTER.decrypt(encrypted)).isEqualTo(plaintext);
  }

  @Test
  public void decrypt_fallsBackToLegacyEncryption() {
    byte[] plaintext = "legacy session data".getBytes(StandardCharsets.UTF_8);
    byte[] legacyEncrypted = shiroEncrypt(plaintext, LEGACY_KEY);

    assertThat(COMBINED_ENCRYPTER.decrypt(legacyEncrypted)).isEqualTo(plaintext);
  }

  @Test
  public void decrypt_roundTripWithCombinedEncrypter() {
    byte[] plaintext = "round trip test".getBytes(StandardCharsets.UTF_8);

    byte[] encrypted = COMBINED_ENCRYPTER.encrypt(plaintext);

    assertThat(COMBINED_ENCRYPTER.decrypt(encrypted)).isEqualTo(plaintext);
  }

  @Test
  public void decrypt_failsWhenNeitherEncrypterCanDecrypt() {
    byte[] wrongKey = CiviFormSessionStoreFactory.deriveLegacyAesKey("wrong-secret");
    byte[] encryptedWithWrongKey = shiroEncrypt("test".getBytes(StandardCharsets.UTF_8), wrongKey);

    assertThatThrownBy(() -> COMBINED_ENCRYPTER.decrypt(encryptedWithWrongKey))
        .isInstanceOf(RuntimeException.class);
  }

  @Test
  public void decrypt_handlesEmptyData() {
    byte[] plaintext = new byte[0];

    byte[] encrypted = COMBINED_ENCRYPTER.encrypt(plaintext);

    assertThat(COMBINED_ENCRYPTER.decrypt(encrypted)).isEqualTo(plaintext);
  }
}
