package services.openApi.v2.serializers;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;
import services.openApi.v2.DefinitionType;
import services.openApi.v2.Header;
import services.openApi.v2.HttpStatusCode;
import services.openApi.v2.Response;

public class ResponseSerializerTest extends OpenApiSerializationAsserter {
  @Test
  public void canSerializeFullObject() throws JsonProcessingException {
    Response model =
        Response.builder(HttpStatusCode.OK, "lorem ipsum")
            .setSchema("#/definitions/name")
            .addHeader(Header.builder(DefinitionType.STRING, "name1").build())
            .build();

    String expected =
        new YamlFormatter()
            .appendLine("\"200\":")
            .appendLine("  description: lorem ipsum")
            .appendLine("  headers:")
            .appendLine("    name1:")
            .appendLine("      type: string")
            .appendLine("  schema:")
            .appendLine("    $ref: '#/definitions/name'")
            .toString();

    assertSerialization(new TestObjectContainer(model), expected);
  }

  @Test
  public void canSerializeEmptyObject() throws JsonProcessingException {
    Response model = Response.builder(HttpStatusCode.OK, "lorem ipsum").build();

    String expected =
        new YamlFormatter()
            .appendLine("\"200\":")
            .appendLine("  description: lorem ipsum")
            .toString();

    assertSerialization(new TestObjectContainer(model), expected);
  }
}
