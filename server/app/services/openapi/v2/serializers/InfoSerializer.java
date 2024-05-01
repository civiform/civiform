package services.openapi.v2.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import services.openapi.v2.Info;

public final class InfoSerializer extends OpenApiSchemaSerializer<Info> {

  public InfoSerializer() {
    this(null);
  }

  public InfoSerializer(Class<Info> t) {
    super(t);
  }

  @Override
  public void serialize(Info value, JsonGenerator gen, SerializerProvider provider)
      throws IOException {
    // open root
    gen.writeStartObject();

    gen.writeStringField("title", value.getTitle());
    gen.writeStringField("version", value.getVersion());
    writeStringFieldIfPresent(gen, "description", value.getDescription());
    writeObjectFieldIfPresent(gen, "contact", value.getContact());

    // close root
    gen.writeEndObject();
  }
}
