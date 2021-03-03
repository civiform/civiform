package services.program;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class Column {
  public abstract String header();

  public abstract String jsonPath();

  public static Column.Builder builder() {
    return new AutoValue_Column.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setHeader(String header);

    public abstract Builder setJsonPath(String jsonPath);

    public abstract Column build();
  }
}
