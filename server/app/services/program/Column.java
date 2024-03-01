package services.program;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import java.util.Optional;
import services.Path;

/** Represents a data column in a CSV export file. */
@AutoValue
@JsonDeserialize(builder = AutoValue_Column.Builder.class)
public abstract class Column {
  @JsonProperty("header")
  public abstract String header();

  @JsonProperty("jsonPath")
  public abstract Optional<Path> jsonPath();

  // Represent the admin name for an option in a multi-select multi-option question (e.g. Checkbox
  // Questions)
  @JsonProperty("optionAdminName")
  public abstract Optional<String> optionAdminName();

  @JsonProperty("columnType")
  public abstract ColumnType columnType();

  public static Column.Builder builder() {
    return new AutoValue_Column.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    @JsonProperty("header")
    public abstract Builder setHeader(String header);

    @JsonProperty("jsonPath")
    public abstract Builder setJsonPath(Path jsonPath);

    @JsonProperty("optionAdminName")
    public abstract Builder setOptionAdminName(String optionAdminName);

    @JsonProperty("columnType")
    public abstract Builder setColumnType(ColumnType columnType);

    public abstract Column build();
  }
}
