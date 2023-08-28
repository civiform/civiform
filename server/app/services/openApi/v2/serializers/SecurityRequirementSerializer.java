package services.openApi.v2.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import services.openApi.v2.SecurityRequirement;

public final class SecurityRequirementSerializer
    extends OpenApiSchemaSerializer<SecurityRequirement> {

  public SecurityRequirementSerializer() {
    this(null);
  }

  public SecurityRequirementSerializer(Class<SecurityRequirement> t) {
    super(t);
  }

  @Override
  public void serialize(SecurityRequirement value, JsonGenerator gen, SerializerProvider provider)
      throws IOException {
    // open root
    gen.writeStartObject();

    gen.writeArrayFieldStart(value.getSecurityType().getLabel());
    gen.writeEndArray();

    // close root
    gen.writeEndObject();
  }
}
