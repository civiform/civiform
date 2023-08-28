package services.openApi.v2.serializers;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;
import services.openApi.v2.Contact;

public class ContactSerializerTest extends OpenApiSerializationAsserter {
  @Test
  public void canSerializeFullObject() throws JsonProcessingException {
    Contact model = Contact.builder().setName("name1").setEmail("email@example.com").build();

    String expected =
        new YamlFormatter()
            .appendLine("name: name1")
            .appendLine("email: email@example.com")
            .toString();

    assertSerialization(model, expected);
  }

  @Test
  public void canSerializeEmptyObject() throws JsonProcessingException {
    Contact model = Contact.builder().build();
    String expected = new YamlFormatter().appendLine("{}").toString();

    assertSerialization(model, expected);
  }
}
