package services.openapi.v2;

import com.google.auto.value.AutoValue;
import java.util.Optional;

/**
 * Allows adding meta data to a single tag that is used by the Operation Object. It is not mandatory
 * to have a Tag Object per tag used there.
 *
 * <p>https://swagger.io/specification/v2/#tag-object
 */
@AutoValue
public abstract class Tag {
  /**
   * Required.
   *
   * <p>The name of the tag.
   */
  public abstract String getName();

  /** A short description for the tag. GFM syntax can be used for rich text representation. */
  public abstract Optional<String> getDescription();

  public static Tag.Builder builder(String name) {
    return new AutoValue_Tag.Builder().setName(name);
  }

  @AutoValue.Builder
  public abstract static class Builder {
    protected abstract Tag.Builder setName(String name);

    public abstract Tag.Builder setDescription(String description);

    public abstract Tag build();
  }
}
