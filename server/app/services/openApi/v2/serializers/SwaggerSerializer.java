package services.openApi.v2.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import services.openApi.v2.Definition;
import services.openApi.v2.Scheme;
import services.openApi.v2.SecurityDefinition;
import services.openApi.v2.SecurityRequirement;
import services.openApi.v2.Swagger;
import services.openApi.v2.Tag;

public final class SwaggerSerializer extends OpenApiSchemaSerializer<Swagger> {

  public SwaggerSerializer() {
    this(null);
  }

  public SwaggerSerializer(Class<Swagger> t) {
    super(t);
  }

  @Override
  public void serialize(Swagger value, JsonGenerator gen, SerializerProvider provider)
      throws IOException {
    // open root
    gen.writeStartObject();

    // fields
    gen.writeStringField("swagger", value.getSwagger());

    writeStringFieldIfPresent(gen, "basePath", value.getBasePath());
    writeStringFieldIfPresent(gen, "host", value.getHost());

    // info
    gen.writeObjectField("info", value.getInfo());

    // schemes
    if (shouldWriteList(value.getSchemes())) {
      gen.writeArrayFieldStart("schemes");
      for (Scheme scheme : value.getSchemes()) {
        gen.writeString(scheme.toString());
      }
      gen.writeEndArray();
    }

    // security
    if (shouldWriteList(value.getSecurityRequirements())) {
      gen.writeArrayFieldStart("security");
      for (SecurityRequirement securityRequirement : value.getSecurityRequirements()) {
        gen.writeObject(securityRequirement);
      }
      gen.writeEndArray();
    }

    // securityDefinitions
    if (shouldWriteList(value.getSecurityDefinitions())) {
      gen.writeObjectFieldStart("securityDefinitions");
      for (SecurityDefinition securityDefinition : value.getSecurityDefinitions()) {
        gen.writeObjectField(securityDefinition.getLabel(), securityDefinition);
      }
      gen.writeEndObject();
    }

    // tags
    if (shouldWriteList(value.getTags())) {
      gen.writeArrayFieldStart("tags");
      for (Tag tag : value.getTags()) {
        gen.writeObject(tag);
      }
      gen.writeEndArray();
    }

    // paths
    if (value.getPaths().isPresent() && shouldWriteList(value.getPaths().get().getPathItems())) {
      gen.writeObjectField("paths", value.getPaths().get());
    }

    // definitions
    if (shouldWriteList(value.getDefinitions())) {
      gen.writeObjectFieldStart("definitions");
      for (Definition definition : value.getDefinitions()) {
        gen.writeObject(definition);
      }
      gen.writeEndObject();
    }

    // close root
    gen.writeEndObject();
  }
}
