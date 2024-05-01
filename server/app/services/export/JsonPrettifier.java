package services.export;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/** Provides methods related to "prettifying" (formatting) JSON strings. */
public final class JsonPrettifier {
  private static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper()
          .enable(SerializationFeature.INDENT_OUTPUT)
          .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

  /**
   * Pretty-print the JSON document
   *
   * @param jsonObject the JSON document as an {@link Object}
   * @return the pretty-printed document
   */
  public static String asPrettyJsonString(Object jsonObject) {
    try {
      return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
    } catch (JsonProcessingException e) {
      return "Error parsing json";
    }
  }

  /**
   * Pretty-print the JSON document
   *
   * @param jsonString the JSON document as a {@link String}
   * @return the pretty-printed document
   */
  public static String asPrettyJsonString(String jsonString) {
    try {
      Object jsonObject = OBJECT_MAPPER.readValue(jsonString, Object.class);
      return asPrettyJsonString(jsonObject);
    } catch (JsonProcessingException e) {
      return "Error parsing json";
    }
  }
}
