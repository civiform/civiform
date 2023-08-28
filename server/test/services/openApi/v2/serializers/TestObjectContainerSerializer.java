package services.openApi.v2.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;

/**
 * Some serializers don't directly start as a new field and thus will throw JsonGenerationException:
 * Can not write a field name, expecting a value
 *
 * <p>This class acts as a small container to test the serialization of a specific object
 */
public class TestObjectContainerSerializer extends StdSerializer<TestObjectContainer> {

  public TestObjectContainerSerializer() {
    this(null);
  }

  public TestObjectContainerSerializer(Class<TestObjectContainer> t) {
    super(t);
  }

  @Override
  public void serialize(TestObjectContainer value, JsonGenerator gen, SerializerProvider provider)
      throws IOException {
    gen.writeStartObject();

    if (value.getHeader() != null) {
      gen.writeObject(value.getHeader());
    }

    if (value.getResponse() != null) {
      gen.writeObject(value.getResponse());
    }

    if (value.getSchema() != null) {
      gen.writeObject(value.getSchema());
    }

    // close root
    gen.writeEndObject();
  }
}
