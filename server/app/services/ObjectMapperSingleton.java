package services;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Singleton to provide access to {@link ObjectMapper} with application-wide settings applied.
 *
 * <p>Prefer getting the instance with {@code @Inject} on the class constructor. Primary use cases
 * for calling this are places in old code that can't be retrofitted easily.
 */
public final class ObjectMapperSingleton {
  private static final ObjectMapper INSTANCE = createObjectMapper();

  private ObjectMapperSingleton() {
    // Prevent instantiation
  }

  /** Creates and configures the ObjectMapper instance. */
  private static ObjectMapper createObjectMapper() {
    return JsonMapper.builder()
        .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        // This adds support for Optional
        .addModule(new Jdk8Module())
        // This adds support for ImmutableList, ImmutableSet, etc.
        .addModule(new GuavaModule())
        // This adds support for Instant, LocalDateTime, java.time classes, etc.
        .addModule(new JavaTimeModule())
        .build();
  }

  /**
   * Get the instance of {@link ObjectMapper}.
   *
   * <p>Prefer getting the instance with {@code @Inject} on the class constructor. Primary use cases
   * for calling this are places in old code that can't be retrofitted easily.
   *
   * <p>Callers must NOT make changes directly to the {@link ObjectMapper} configuration with this
   * instance as it will change settings for the entire application.
   *
   * <p>To make targeted configuration customizations call {@link #createCopy()} instead.
   */
  public static ObjectMapper instance() {
    return INSTANCE;
  }

  /**
   * Create a copy of the {@link ObjectMapper} instance.
   *
   * <p>Use this if you need to apply additional configuration specific to certain use cases without
   * affecting the global object.
   */
  public static ObjectMapper createCopy() {
    return INSTANCE.copy();
  }

  /**
   * Create a copy of {@link ObjectMapper} instance.
   *
   * <p>This is NOT to be used by new code and is here for backwards compatibility with old code. If
   * customizing the {@link ObjectMapper} is needed use {@link #createCopy()} for new code.
   */
  public static ObjectMapper createLegacyCopy() {
    return INSTANCE
        .copy()
        // Reset new settings back to default
        .disable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
        .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        // Old settings
        .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
  }
}
