package services.openApi.v2.serializers;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;
import services.openApi.v2.DefinitionType;
import services.openApi.v2.Format;
import services.openApi.v2.Header;

public class HeaderSerializerTest extends OpenApiSerializationAsserter {
  @Test
  public void canSerializeFullObject() throws JsonProcessingException {
    Header model =
        Header.builder(DefinitionType.INTEGER, "name1")
            .setFormat(Format.INT32)
            .setDescription("lorem ipsum")
            .build();

    String expected =
        new YamlFormatter()
            .appendLine("name1:")
            .appendLine("  type: integer")
            .appendLine("  format: int32")
            .appendLine("  description: lorem ipsum")
            .toString();

    assertSerialization(new TestObjectContainer(model), expected);
  }

  @Test
  public void canSerializeEmptyObject() throws JsonProcessingException {
    Header model = Header.builder(DefinitionType.STRING, "name1").build();

    String expected = "name1:\n" + "  type: string\n";

    assertSerialization(new TestObjectContainer(model), expected);
  }
}
