package services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Utility class providing unchecked extensions for Jackson ObjectMapper operations. */
public final class JsonUtils {

  private JsonUtils() {}

  /** Unchecked version of ObjectMapper.writeValueAsString(Object). */
  public static String writeValueAsString(ObjectMapper objectMapper, Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
