package services.openapi.v2.serializers;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;
import services.openapi.v2.SecurityRequirement;
import services.openapi.v2.SecurityType;

public class SecurityRequirementSerializerTest extends OpenApiSerializationAsserter {
  @Test
  public void canSerializeFullObject() throws JsonProcessingException {
    SecurityRequirement model = SecurityRequirement.builder(SecurityType.BASIC).build();

    String expected = new YamlFormatter().appendLine("basicAuth: []").toString();

    assertSerialization(model, expected);
  }
}
