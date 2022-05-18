package services;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import java.util.Base64;

public class EncryptionUtils {

  /** Apply the HMAC-SHA-256 hashing function to the input using the provided key. */
  public static String sign(String message, String key) {
    byte[] rawMessage = Base64.getDecoder().decode(message);
    byte[] rawKey = Base64.getDecoder().decode(key);

    HashFunction hashFunction = Hashing.hmacSha256(rawKey);
    HashCode saltedMessage = hashFunction.hashBytes(rawMessage);

    return Base64.getEncoder().encodeToString(saltedMessage.asBytes());
  }
}
