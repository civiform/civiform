package services.openApi.v2.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import services.openApi.v2.SecurityDefinition;
import services.openApi.v2.SecurityType;

public final class SecurityDefinitionSerializer
    extends OpenApiSchemaSerializer<SecurityDefinition> {

  public SecurityDefinitionSerializer() {
    this(null);
  }

  public SecurityDefinitionSerializer(Class<SecurityDefinition> t) {
    super(t);
  }

  @Override
  public void serialize(SecurityDefinition value, JsonGenerator gen, SerializerProvider provider)
      throws IOException {
    // open root
    gen.writeStartObject();

    gen.writeStringField("type", value.getType());

    // Api Key
    if (value.getSecurityType() == SecurityType.API_KEY) {
      writeStringFieldIfPresent(gen, "name", value.getName());
      writeEnumFieldIfPresent(gen, "in", value.getIn());
    }

    writeStringFieldIfPresent(gen, "description", value.getDescription());

    // close root
    gen.writeEndObject();
  }
}
