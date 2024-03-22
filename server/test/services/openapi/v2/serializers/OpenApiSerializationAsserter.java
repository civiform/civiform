package services.openapi.v2.serializers;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.security.InvalidParameterException;

public abstract class OpenApiSerializationAsserter {
  protected <T> void assertSerialization(T model, String expected) throws JsonProcessingException {
    ObjectMapper mapper = Swagger2YamlMapper.getMapper();
    mapper.registerModules(
        new SimpleModule()
            .addSerializer(TestObjectContainer.class, new TestObjectContainerSerializer()));

    String result = mapper.writeValueAsString(model);

    assertThat(result).isEqualTo(expected);
  }

  /**
   * Small wrapper around StringBuilder.
   *
   * <p>This is really only here because our java formatter thinks it knows better and screws up the
   * formatting of my multiline strings making it so they are not easily parseable by a human.
   */
  public static class YamlFormatter {
    private final StringBuilder sb = new StringBuilder();

    // Add a line to the string builder with a line break
    public YamlFormatter appendLine(String message) {
      if (!validateLeadSpaces(message)) {
        throw new InvalidParameterException(
            String.format("Expecting leading spaces to come in pairs `%s`", message));
      }

      sb.append(message).append("\n");

      return this;
    }

    /** Just a quick check that the left padding is an even number */
    private Boolean validateLeadSpaces(String message) {
      int count = 0;

      for (int i = 0; i < message.length(); i++) {
        char c = message.charAt(i);
        if (c == ' ') {
          count++;
        } else {
          break;
        }
      }

      return count % 2 == 0;
    }

    @Override
    public String toString() {
      return sb.toString();
    }
  }
}
