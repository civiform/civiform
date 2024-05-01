package services.openapi.v2.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import services.openapi.v2.MimeType;
import services.openapi.v2.Operation;
import services.openapi.v2.Parameter;
import services.openapi.v2.Response;

public final class OperationSerializer extends OpenApiSchemaSerializer<Operation> {

  public OperationSerializer() {
    this(null);
  }

  public OperationSerializer(Class<Operation> t) {
    super(t);
  }

  @Override
  public void serialize(Operation value, JsonGenerator gen, SerializerProvider provider)
      throws IOException {
    // open root
    gen.writeStartObject();

    writeStringFieldIfPresent(gen, "summary", value.getSummary());
    writeStringFieldIfPresent(gen, "operationId", value.getOperationId());
    writeStringFieldIfPresent(gen, "description", value.getDescription());

    // parameters
    if (shouldWriteList(value.getParameters())) {
      gen.writeArrayFieldStart("parameters");
      for (Parameter parameter : value.getParameters()) {
        gen.writeObject(parameter);
      }
      gen.writeEndArray();
    }

    // produces
    if (shouldWriteList(value.getProduces())) {
      gen.writeArrayFieldStart("produces");
      for (MimeType mineType : value.getProduces()) {
        gen.writeObject(mineType.getCode());
      }
      gen.writeEndArray();
    }

    // responses
    if (shouldWriteList(value.getResponses())) {
      gen.writeObjectFieldStart("responses");
      for (Response response : value.getResponses()) {
        gen.writeObject(response);
      }
      gen.writeEndObject();
    }

    // tags
    if (shouldWriteList(value.getTags())) {
      gen.writeArrayFieldStart("tags");
      for (String tag : value.getTags()) {
        gen.writeString(tag);
      }
      gen.writeEndArray();
    }

    // close root
    gen.writeEndObject();
  }
}
