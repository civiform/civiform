package services.program;

import com.google.auto.value.AutoValue;
import java.util.Optional;
import services.Path;

@AutoValue
public abstract class Column {
  public abstract String header();

  public abstract Optional<String> answerDataKey();

  public abstract Optional<Path> jsonPath();

  public abstract ColumnType columnType();

  public static Column.Builder builder() {
    return new AutoValue_Column.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setHeader(String header);

    public abstract Builder setAnswerDataKey(String answerDataKey);

    public abstract Builder setJsonPath(Path jsonPath);

    public abstract Builder setColumnType(ColumnType columnType);

    public abstract Column build();
  }
}
