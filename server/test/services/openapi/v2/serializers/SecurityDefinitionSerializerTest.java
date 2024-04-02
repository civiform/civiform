package services.openapi.v2.serializers;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;
import services.openapi.v2.ApiKeyLocation;
import services.openapi.v2.SecurityDefinition;

public class SecurityDefinitionSerializerTest extends OpenApiSerializationAsserter {
  @Test
  public void canSerializeFullObjectBasic() throws JsonProcessingException {
    SecurityDefinition model =
        SecurityDefinition.basicBuilder().setDescription("lorem ipsum").build();

    String expected =
        new YamlFormatter()
            .appendLine("type: basic")
            .appendLine("description: lorem ipsum")
            .toString();

    assertSerialization(model, expected);
  }

  @Test
  public void canSerializeFullObjectApiKey() throws JsonProcessingException {
    SecurityDefinition model =
        SecurityDefinition.apiKeyBuilder("name1", ApiKeyLocation.HEADER)
            .setDescription("lorem ipsum")
            .build();

    String expected =
        new YamlFormatter()
            .appendLine("type: apiKey")
            .appendLine("name: name1")
            .appendLine("in: header")
            .appendLine("description: lorem ipsum")
            .toString();

    assertSerialization(model, expected);
  }

  @Test
  public void canSerializeEmptyObject() throws JsonProcessingException {
    SecurityDefinition model = SecurityDefinition.basicBuilder().build();

    String expected = new YamlFormatter().appendLine("type: basic").toString();

    assertSerialization(model, expected);
  }
}
