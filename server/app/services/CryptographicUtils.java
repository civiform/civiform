package services;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class CryptographicUtils {

  /** Apply the HMAC-SHA-256 hashing function to the input using the provided key. */
  public static String sign(String message, String key) {
    byte[] rawMessage = message.getBytes(StandardCharsets.UTF_8);
    byte[] rawKey = key.getBytes(StandardCharsets.UTF_8);

    HashFunction hashFunction = Hashing.hmacSha256(rawKey);
    HashCode saltedMessage = hashFunction.hashBytes(rawMessage);

    return Base64.getEncoder().encodeToString(saltedMessage.asBytes());
  }
}
