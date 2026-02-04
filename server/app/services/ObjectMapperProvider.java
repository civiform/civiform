package services;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    ObjectMapper mapper = ObjectMapperSingleton.instance();

    // Needs to set to Json helper
    Json.setObjectMapper(mapper);

    return mapper;
  }
}
