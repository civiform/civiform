package services.openapi.v2.serializers;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;
import services.openapi.v2.Definition;
import services.openapi.v2.DefinitionType;
import services.openapi.v2.Format;

public class DefinitionSerializerTest extends OpenApiSerializationAsserter {
  @Test
  public void canSerializeFullObject() throws JsonProcessingException {
    Definition model =
        Definition.builder("name1", DefinitionType.INTEGER)
            .setFormat(Format.INT32)
            .addDefinition(Definition.builder("name2", DefinitionType.STRING).build())
            .build();

    String expected =
        new YamlFormatter()
            .appendLine("name1:")
            .appendLine("  type: integer")
            .appendLine("  format: int32")
            .appendLine("  properties:")
            .appendLine("    name2:")
            .appendLine("      type: string")
            .toString();

    assertSerialization(new TestObjectContainer(model), expected);
  }

  @Test
  public void canSerializeEmptyObject() throws JsonProcessingException {
    Definition model = Definition.builder("name1", DefinitionType.STRING).build();

    String expected =
        new YamlFormatter().appendLine("name1:").appendLine("  type: string").toString();

    assertSerialization(new TestObjectContainer(model), expected);
  }
}
