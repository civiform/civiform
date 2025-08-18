package services;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.inject.Provider;
import play.libs.Json;

/**
 * Configures a customized version of the ObjectMapper that includes support for additional common
 * types such as: {@link java.util.Optional}, {@link com.google.common.collect.ImmutableList},
 * {@link com.google.common.collect.ImmutableMap}.
 */
public class ObjectMapperProvider implements Provider<ObjectMapper> {

  @Override
  public ObjectMapper get() {
    ObjectMapper mapper =
        new ObjectMapper()
            // Play defaults
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            // This adds support for Optional
            .registerModule(new Jdk8Module())
            // This adds support for ImmutableList, ImmutableSet, etc.
            .registerModule(new GuavaModule())
            // This adds support for Instant, LocalDateTime, java.time classes, etc.
            .registerModule(new JavaTimeModule());

    // Needs to set to Json helper
    Json.setObjectMapper(mapper);

    return mapper;
  }
}
