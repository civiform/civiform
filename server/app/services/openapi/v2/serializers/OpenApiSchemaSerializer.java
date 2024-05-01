package services.openapi.v2.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.Optional;

public abstract class OpenApiSchemaSerializer<T> extends StdSerializer<T> {
  public OpenApiSchemaSerializer(Class<T> t) {
    super(t);
  }

  /** Convenience method for writing optional string values. */
  protected void writeStringFieldIfPresent(JsonGenerator gen, String fieldName, String value)
      throws IOException {
    if (value == null || value.isEmpty()) {
      return;
    }

    gen.writeStringField(fieldName, value);
  }

  /** Convenience method for writing optional string values. */
  protected <U> void writeStringFieldIfPresent(
      JsonGenerator gen, String fieldName, Optional<U> value) throws IOException {
    if (value.isEmpty() || value.get().toString().isEmpty()) {
      return;
    }

    gen.writeStringField(fieldName, value.get().toString());
  }

  /** Convenience method for writing optional object values. */
  protected <U> void writeObjectFieldIfPresent(
      JsonGenerator gen, String fieldName, Optional<U> value) throws IOException {
    if (value.isEmpty()) {
      return;
    }

    gen.writeObjectField(fieldName, value.get());
  }

  /** Convenience method for writing optional object values. */
  protected <U> void writeEnumFieldIfPresent(JsonGenerator gen, String fieldName, Optional<U> value)
      throws IOException {
    if (value.isEmpty()) {
      return;
    }

    gen.writeObjectField(fieldName, value.get().toString());
  }

  /** Determines if the list should be written by the serializer */
  protected <U> Boolean shouldWriteList(ImmutableList<U> list) {
    return !list.isEmpty();
  }
}
