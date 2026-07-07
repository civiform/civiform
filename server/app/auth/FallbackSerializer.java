package auth;

import org.pac4j.core.util.serializer.JavaSerializer;
import org.pac4j.core.util.serializer.JsonSerializer;
import org.pac4j.core.util.serializer.Serializer;

/**
 * Pac4j has deprecated the {@link JavaSerializer} in v6.5.0, so we are stuck on v6.4.x until we are
 * ready to remove the {@link JavaSerializer}.
 *
 * <p>This custom {@link Serializer} is used to provide transition time for existing user sessions
 * to migrate from the {@link JavaSerializer} to the {@link JsonSerializer}. It follows the same
 * pattern used for the {@link FallbackDataEncrypter}.
 */
public class FallbackSerializer implements Serializer {
  private final Serializer primary;
  private final Serializer fallback;

  public FallbackSerializer(Serializer primary, Serializer fallback) {
    this.primary = primary;
    this.fallback = fallback;
  }

  @Override
  public byte[] serializeToBytes(Object o) {
    return primary.serializeToBytes(o);
  }

  @Override
  public String serializeToString(Object o) {
    return primary.serializeToString(o);
  }

  @Override
  public Object deserializeFromBytes(byte[] bytes) {
    // pac4j's AbstractSerializer.deserializeFromBytes catches parse exceptions internally
    // and returns null. So we fallback on null OR a thrown exception.
    try {
      Object result = primary.deserializeFromBytes(bytes);
      if (result != null) {
        return result;
      }
    } catch (RuntimeException e) {
      // fall through to fallback
    }
    return fallback.deserializeFromBytes(bytes);
  }

  @Override
  public Object deserializeFromString(String value) {
    try {
      Object result = primary.deserializeFromString(value);
      if (result != null) {
        return result;
      }
    } catch (RuntimeException e) {
      // fall through to fallback
    }
    return fallback.deserializeFromString(value);
  }
}
