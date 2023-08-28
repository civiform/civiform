package services.openApi.v2;

import com.google.auto.value.AutoValue;
import java.util.Optional;

/**
 * https://swagger.io/specification/v2/#parameter-object
 *
 * <p>Describes a single operation parameter.
 */
@AutoValue
public abstract class Parameter {
  /**
   * Required.
   *
   * <p>The name of the parameter. Parameter names are case sensitive.
   *
   * <p>If in is "path", the name field MUST correspond to the associated path segment from the path
   * field in the Paths Object. See Path Templating for further information.
   *
   * <p>For all other cases, the name corresponds to the parameter name used based on the in
   * property.
   */
  public abstract String getName();

  /**
   * Required.
   *
   * <p>The location of the parameter. Possible values are "query", "header", "path", "formData" or
   * "body".
   */
  public abstract In getIn();

  public abstract Optional<String> getDescription();

  /**
   * A brief description of the parameter. This could contain examples of use. GFM syntax can be
   * used for rich text representation.
   */
  public abstract Optional<Format> getFormat();

  /**
   * Determines whether this parameter is mandatory. If the parameter is in "path", this property is
   * required and its value MUST be true. Otherwise, the property MAY be included and its default
   * value is false.
   */
  public abstract Boolean getRequired();

  /**
   * Required.
   *
   * <p>The type of the parameter. Since the parameter is not located at the request body, it is
   * limited to simple types (that is, not an object). The value MUST be one of "string", "number",
   * "integer", "boolean", "array" or "file". If type is "file", the consumes MUST be either
   * "multipart/form-data", " application/x-www-form-urlencoded" or both and the parameter MUST be
   * in "formData".
   */
  public abstract DefinitionType getDefinitionType();

  public static Parameter.Builder builder(String name, In in, DefinitionType type) {
    return new AutoValue_Parameter.Builder()
        .setName(name)
        .setIn(in)
        .setDefinitionType(type)
        .setRequired(false);
  }

  @AutoValue.Builder
  public abstract static class Builder {
    protected abstract Parameter.Builder setName(String name);

    protected abstract Parameter.Builder setIn(In in);

    protected abstract Parameter.Builder setDefinitionType(DefinitionType type);

    protected abstract Parameter.Builder setRequired(Boolean required);

    public abstract Parameter.Builder setFormat(Format format);

    public abstract Parameter.Builder setDescription(String description);

    public final Parameter.Builder markAsRequired() {
      this.setRequired(true);
      return this;
    }

    public abstract Parameter build();
  }
}
