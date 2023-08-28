package services.openApi.v2.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import services.openApi.v2.Header;
import services.openApi.v2.Response;

public final class ResponseSerializer extends OpenApiSchemaSerializer<Response> {

  public ResponseSerializer() {
    this(null);
  }

  public ResponseSerializer(Class<Response> t) {
    super(t);
  }

  @Override
  public void serialize(Response value, JsonGenerator gen, SerializerProvider provider)
      throws IOException {
    // open root
    gen.writeObjectFieldStart(value.getHttpStatusCode().toString());
    gen.writeStringField("description", value.getDescription());

    // headers
    if (shouldWriteList(value.getHeaders())) {
      gen.writeObjectFieldStart("headers");
      for (Header header : value.getHeaders()) {
        gen.writeObject(header);
      }
      gen.writeEndObject();
    }

    // schema
    if (value.getSchema().isPresent()) {
      gen.writeObjectFieldStart("schema");
      writeStringFieldIfPresent(gen, "$ref", value.getSchema());
      gen.writeEndObject();
    }

    // close root
    gen.writeEndObject();
  }
}
