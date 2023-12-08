package views;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.collect.ImmutableMap;

public abstract class HtmxVals {
  private static final ObjectMapper MAPPER =
      new ObjectMapper().registerModule(new GuavaModule()).registerModule(new Jdk8Module());

  public static String serializeVals(String... keyValuePairs) {
    if (keyValuePairs.length % 2 != 0) {
      throw new RuntimeException("");
    }

    ImmutableMap.Builder<String, String> result = ImmutableMap.builder();

    for (int i = 0; i < keyValuePairs.length; i += 2) {
      result.put(keyValuePairs[i], keyValuePairs[i + 1]);
    }

    try {
      return MAPPER.writeValueAsString(result.build());
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
