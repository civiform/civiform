package services.openapi.v2.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import services.openapi.v2.Tag;

public final class TagSerializer extends OpenApiSchemaSerializer<Tag> {

  public TagSerializer() {
    this(null);
  }

  public TagSerializer(Class<Tag> t) {
    super(t);
  }

  @Override
  public void serialize(Tag value, JsonGenerator gen, SerializerProvider provider)
      throws IOException {
    // open root
    gen.writeStartObject();

    gen.writeStringField("name", value.getName());
    writeStringFieldIfPresent(gen, "description", value.getDescription());

    // close root
    gen.writeEndObject();
  }
}
