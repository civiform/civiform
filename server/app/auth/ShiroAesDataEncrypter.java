package auth;

import org.apache.shiro.crypto.AesCipherService;
import org.pac4j.core.util.CommonHelper;
import org.pac4j.play.store.DataEncrypter;

/**
 * This is lifted directly from
 * https://github.com/pac4j/play-pac4j/blob/12.0.x-PLAY3.0/shared/src/main/java/org/pac4j/play/store/ShiroAesDataEncrypter.java,
 * but with encryption bits removed as this is intended ONLY to be used to migrate old sessions to
 * use the new pac4j 13 JdkAesDataEncrypter.
 *
 * <p>UNDER NO CIRCUMSTANCES SHOULD YOU USE THIS CLASS FOR ANY PURPOSE THAN WHAT IT IS CURRENTLY
 * BEING USED FOR!
 */
public final class ShiroAesDataEncrypter implements DataEncrypter {
  private final AesCipherService aesCipherService = new AesCipherService();
  private final byte[] key;

  public ShiroAesDataEncrypter(final byte[] key) {
    CommonHelper.assertNotNull("key", key);
    this.key = key.clone();
  }

  @Override
  public byte[] decrypt(byte[] encryptedBytes) {
    if (encryptedBytes == null) {
      return null;
    } else {
      return aesCipherService.decrypt(encryptedBytes, key).getBytes();
    }
  }

  @Override
  public byte[] encrypt(byte[] rawBytes) {
    throw new UnsupportedOperationException(
        "Legacy Shiro encrypter must never be used to encrypt new sessions. This is a decrypt-only"
            + " migration aid.");
  }
}
