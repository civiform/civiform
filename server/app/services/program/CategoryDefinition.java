package services.program;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonDeserialize(builder = AutoValue_CategoryDefinition.Builder.class)
public abstract class CategoryDefinition {

  /** Unique identifier for a CategoryDefinition. */
  @JsonProperty("id")
  public abstract long id();

  /** The unique category name in the default locale, which is "en_US". */
  @JsonProperty("defaultName")
  public abstract String defaultName();

  public static CategoryDefinition.Builder builder() {
    return new AutoValue_CategoryDefinition.Builder();
  }

  public abstract CategoryDefinition.Builder toBuilder();

  @AutoValue.Builder
  public abstract static class Builder {

    @JsonProperty("id")
    public abstract CategoryDefinition.Builder setId(long id);

    @JsonProperty("defaultName")
    public abstract CategoryDefinition.Builder setDefaultName(String value);

    public abstract CategoryDefinition build();
  }
}
