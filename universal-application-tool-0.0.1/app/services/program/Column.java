package services.program;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import java.util.Optional;
import services.Path;

@AutoValue
@JsonDeserialize(builder = AutoValue_Column.Builder.class)
public abstract class Column {
  @JsonProperty("header")
  public abstract String header();

  @JsonProperty("answerDataKey")
  public abstract Optional<String> answerDataKey();

  @JsonProperty("jsonPath")
  public abstract Optional<Path> jsonPath();

  @JsonProperty("columnType")
  public abstract ColumnType columnType();

  public static Column.Builder builder() {
    return new AutoValue_Column.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    @JsonProperty("header")
    public abstract Builder setHeader(String header);

    @JsonProperty("answerDataKey")
    public abstract Builder setAnswerDataKey(String answerDataKey);

    @JsonProperty("jsonPath")
    public abstract Builder setJsonPath(Path jsonPath);

    @JsonProperty("columnType")
    public abstract Builder setColumnType(ColumnType columnType);

    public abstract Column build();
  }
}
