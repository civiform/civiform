package auth;

import java.security.GeneralSecurityException;
import javax.crypto.AEADBadTagException;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import org.pac4j.play.store.DataEncrypter;

/**
 * A class to always use the primary encrypter to encrypt a value, but allow a fallback decrypter if
 * the bytes passed in were encrypted with an alternate encryption scheme. Used to allow decrypting
 * session cookies using an old encryption scheme without invalidating the session, where the
 * session will be encrypted with the new scheme upon update.
 */
public class FallbackDataEncrypter implements DataEncrypter {
  private final DataEncrypter primary;
  private final DataEncrypter fallback;

  public FallbackDataEncrypter(DataEncrypter primary, DataEncrypter fallback) {
    this.primary = primary;
    this.fallback = fallback;
  }

  @Override
  public byte[] encrypt(byte[] value) {
    return primary.encrypt(value);
  }

  @Override
  public byte[] decrypt(byte[] value) {
    try {
      return primary.decrypt(value);
    } catch (RuntimeException e) {
      if (isDecryptFailure(e)) {
        return fallback.decrypt(value);
      }
      throw e;
    }
  }

  /**
   * Drills down into the exception stack to find a recognizable decryption failure.
   *
   * @param t Throwable exception to inspect for a decryption-type exception in the stack.
   * @return Whether a decryption-type exception was found.
   */
  private static boolean isDecryptFailure(Throwable t) {
    Throwable cur = t;
    while (cur != null) {
      if (cur instanceof AEADBadTagException) return true;
      if (cur instanceof BadPaddingException) return true;
      if (cur instanceof IllegalBlockSizeException) return true;
      if (cur instanceof GeneralSecurityException) return true;
      cur = cur.getCause();
    }
    return false;
  }
}
