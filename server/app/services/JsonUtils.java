package services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Utility class providing unchecked extensions for Jackson ObjectMapper operations. */
public final class JsonUtils {

  private JsonUtils() {}

  /** Unchecked version of ObjectMapper.readTree(String). */
  public static JsonNode readTree(ObjectMapper objectMapper, String jsonString) {
    try {
      return objectMapper.readTree(jsonString);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
