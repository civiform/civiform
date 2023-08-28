package services.openApi.v2.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import services.openApi.v2.Contact;

public final class ContactSerializer extends OpenApiSchemaSerializer<Contact> {

  public ContactSerializer() {
    this(null);
  }

  public ContactSerializer(Class<Contact> t) {
    super(t);
  }

  @Override
  public void serialize(Contact value, JsonGenerator gen, SerializerProvider provider)
      throws IOException {
    // open root
    gen.writeStartObject();

    writeStringFieldIfPresent(gen, "name", value.getName());
    writeStringFieldIfPresent(gen, "email", value.getEmail());

    // close root
    gen.writeEndObject();
  }
}
