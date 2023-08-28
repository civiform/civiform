package services.openApi.v2.serializers;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;
import services.openApi.v2.Tag;

public class TagSerializerTest extends OpenApiSerializationAsserter {
  @Test
  public void canSerializeFullObject() throws JsonProcessingException {
    Tag model = Tag.builder("name1").setDescription("lorem ipsum").build();

    String expected =
        new YamlFormatter()
            .appendLine("name: name1")
            .appendLine("description: lorem ipsum")
            .toString();

    assertSerialization(model, expected);
  }

  @Test
  public void canSerializeEmptyObject() throws JsonProcessingException {
    Tag model = Tag.builder("name1").build();

    String expected = new YamlFormatter().appendLine("name: name1").toString();

    assertSerialization(model, expected);
  }
}
