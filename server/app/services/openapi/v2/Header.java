package services.openapi.v2;

import com.google.auto.value.AutoValue;
import java.util.Optional;

/**
 * Lists the headers that can be sent as part of a response.
 *
 * <p>https://swagger.io/specification/v2/#header-object
 */
@AutoValue
public abstract class Header {
  /** The name of the property corresponds to the name of the header */
  public abstract String getName();

  /**
   * Required.
   *
   * <p>The type of the object. The value MUST be one of "string", "number", "integer", "boolean",
   * or "array".
   */
  public abstract DefinitionType getType();

  /** A short description of the header. */
  public abstract Optional<String> getDescription();

  /**
   * The extending format for the previously mentioned type. See Data Type Formats for further
   * details.
   */
  public abstract Optional<Format> getFormat();

  public static Header.Builder builder(DefinitionType definitionType, String name) {
    return new AutoValue_Header.Builder().setType(definitionType).setName(name);
  }

  @AutoValue.Builder
  public abstract static class Builder {
    protected abstract Header.Builder setName(String name);

    protected abstract Header.Builder setType(DefinitionType definitionType);

    public abstract Header.Builder setDescription(String description);

    public abstract Header.Builder setFormat(Format format);

    public abstract Header build();
  }
}
