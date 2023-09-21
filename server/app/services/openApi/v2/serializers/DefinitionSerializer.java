package services.openApi.v2.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.Optional;
import services.openApi.v2.Definition;
import services.openApi.v2.DefinitionType;
import services.openApi.v2.Format;

public final class DefinitionSerializer extends OpenApiSchemaSerializer<Definition> {

  public DefinitionSerializer() {
    this(null);
  }

  public DefinitionSerializer(Class<Definition> t) {
    super(t);
  }

  @Override
  public void serialize(Definition value, JsonGenerator gen, SerializerProvider provider)
      throws IOException {
    // open root
    gen.writeObjectFieldStart(value.getName());
    gen.writeStringField("type", value.getDefinitionType().toString());

    if (canWriteFormat(value.getFormat())) {
      writeEnumFieldIfPresent(gen, "format", value.getFormat());
    }

    if (value.getNullable()) {
      gen.writeBooleanField("x-nullable", value.getNullable());
    }

    // process child schemas
    if (value.getDefinitionType() == DefinitionType.ARRAY) {
      gen.writeObjectFieldStart("items");
      gen.writeStringField("type", DefinitionType.OBJECT.toString());
      processChildDefinitions(value.getDefinitions(), gen);
      gen.writeEndObject();
    } else {
      processChildDefinitions(value.getDefinitions(), gen);
    }

    // close root
    gen.writeEndObject();
  }

  private void processChildDefinitions(
      ImmutableList<Definition> childDefinitions, JsonGenerator gen) throws IOException {
    if (!childDefinitions.isEmpty()) {
      gen.writeObjectFieldStart("properties");
      for (Definition definition : childDefinitions) {
        gen.writeObject(definition);
      }
      gen.writeEndObject();
    }
  }

  /** Determines if the format field is allowed to be written */
  private Boolean canWriteFormat(Optional<Format> format) {
    if (format.isEmpty()) {
      return false;
    }

    return format.get() != Format.STRING && format.get() != Format.ARRAY;
  }
}
