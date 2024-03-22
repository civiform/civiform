package services.openapi.v2.serializers;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;
import services.openapi.v2.DefinitionType;
import services.openapi.v2.In;
import services.openapi.v2.Operation;
import services.openapi.v2.OperationType;
import services.openapi.v2.Parameter;
import services.openapi.v2.PathItem;
import services.openapi.v2.Paths;

public class PathsSerializerTest extends OpenApiSerializationAsserter {
  @Test
  public void canSerializeFullObject() throws JsonProcessingException {
    Paths model =
        Paths.builder()
            .addPathItem(
                PathItem.builder("/ref1")
                    .addOperation(
                        Operation.builder(OperationType.GET).setSummary("summary1").build())
                    .addParameter(
                        Parameter.builder("parameterName1", In.HEADER, DefinitionType.STRING)
                            .build())
                    .build())
            .build();

    String expected =
        new YamlFormatter()
            .appendLine("/ref1:")
            .appendLine("  get:")
            .appendLine("    summary: summary1")
            .appendLine("  parameters:")
            .appendLine("    - in: header")
            .appendLine("      name: parameterName1")
            .appendLine("      type: string")
            .appendLine("      required: false")
            .toString();

    assertSerialization(model, expected);
  }

  @Test
  public void canSerializeEmptyObject() throws JsonProcessingException {
    Paths model = Paths.builder().build();

    String expected = new YamlFormatter().appendLine("{}").toString();

    assertSerialization(model, expected);
  }
}
