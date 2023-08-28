package services.openApi.v2.serializers;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;
import services.openApi.v2.Contact;
import services.openApi.v2.Info;

public class InfoSerializerTest extends OpenApiSerializationAsserter {
  @Test
  public void canSerializeFullObject() throws JsonProcessingException {
    Info model =
        Info.builder("title1", "version1")
            .setDescription("lorem ipsum")
            .setContact(Contact.builder().build())
            .build();

    String expected =
        new YamlFormatter()
            .appendLine("title: title1")
            .appendLine("version: version1")
            .appendLine("description: lorem ipsum")
            .appendLine("contact: {}")
            .toString();

    assertSerialization(model, expected);
  }

  @Test
  public void canSerializeEmptyObject() throws JsonProcessingException {
    Info model = Info.builder("title1", "version1").build();

    String expected =
        new YamlFormatter().appendLine("title: title1").appendLine("version: version1").toString();

    assertSerialization(model, expected);
  }
}
