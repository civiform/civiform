package services.openApi.v2.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.util.Optional;
import services.openApi.v2.DefinitionType;
import services.openApi.v2.Format;
import services.openApi.v2.Parameter;

public final class ParameterSerializer extends OpenApiSchemaSerializer<Parameter> {

  public ParameterSerializer() {
    this(null);
  }

  public ParameterSerializer(Class<Parameter> t) {
    super(t);
  }

  @Override
  public void serialize(Parameter value, JsonGenerator gen, SerializerProvider provider)
      throws IOException {
    // open root
    gen.writeStartObject();

    gen.writeStringField("in", value.getIn().toString());
    gen.writeStringField("name", value.getName());
    writeStringFieldIfPresent(gen, "type", value.getDefinitionType().toString());

    if (canWriteFormat(value.getDefinitionType(), value.getFormat())) {
      writeEnumFieldIfPresent(gen, "format", value.getFormat());
    }

    gen.writeBooleanField("required", value.getRequired());
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
