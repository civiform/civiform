package services.openapi.v2.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.util.Optional;
import services.openapi.v2.DefinitionType;
import services.openapi.v2.Format;
import services.openapi.v2.Header;

public final class HeaderSerializer extends OpenApiSchemaSerializer<Header> {

  public HeaderSerializer() {
    this(null);
  }

  public HeaderSerializer(Class<Header> t) {
    super(t);
  }

  @Override
  public void serialize(Header value, JsonGenerator gen, SerializerProvider provider)
      throws IOException {
    // open root
    gen.writeObjectFieldStart(value.getName());

    gen.writeStringField("type", value.getType().toString());

    if (canWriteFormat(value.getType(), value.getFormat())) {
      writeEnumFieldIfPresent(gen, "format", value.getFormat());
    }

    writeStringFieldIfPresent(gen, "description", value.getDescription());

    // close root
    gen.writeEndObject();
  }

  private Boolean canWriteFormat(DefinitionType definitionType, Optional<Format> format) {
    if (definitionType == DefinitionType.OBJECT || definitionType == DefinitionType.ARRAY) {
      return false;
    }

    return format.isPresent();
  }
}
