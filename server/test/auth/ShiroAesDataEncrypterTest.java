package auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Random;
import org.apache.shiro.crypto.AesCipherService;
import org.junit.Test;

public class ShiroAesDataEncrypterTest {

  /**
   * Generates a legacy 32-byte AES key using the same algorithm that was used before the pac4j 13
   * migration. This mirrors the deriveLegacyAesKey method in CiviFormSessionStoreFactory.
   */
  private static byte[] generateLegacyKey(String secret) {
    Random r = new Random(secret.hashCode());
    byte[] key = new byte[32];
    r.nextBytes(key);
    return key;
  }

  @Test
  public void decrypt_decryptsDataEncryptedWithShiroCipherService() {
    byte[] key = generateLegacyKey("test-secret");
    byte[] plaintext = "Hello, World! This is test data.".getBytes(StandardCharsets.UTF_8);

    // Encrypt using the actual Shiro cipher service (simulating old behavior)
    AesCipherService cipherService = new AesCipherService();
    byte[] encrypted = cipherService.encrypt(plaintext, key).getBytes();

    // Decrypt using our ShiroAesDataEncrypter
    ShiroAesDataEncrypter encrypter = new ShiroAesDataEncrypter(key);
    byte[] decrypted = encrypter.decrypt(encrypted);

    assertThat(decrypted).isEqualTo(plaintext);
  }

  @Test
  public void encrypt_throwsUnsupportedOperationException() {
    byte[] key = generateLegacyKey("test-secret");
    ShiroAesDataEncrypter encrypter = new ShiroAesDataEncrypter(key);

    assertThatThrownBy(() -> encrypter.encrypt("test".getBytes(StandardCharsets.UTF_8)))
        .isInstanceOf(UnsupportedOperationException.class)
        .hasMessageContaining("Legacy Shiro encrypter must never be used to encrypt new sessions")
        .hasMessageContaining("decrypt-only");
  }
}
