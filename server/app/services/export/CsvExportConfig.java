package services.export;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

/** Contains all information needed to export a CSV file for a program. */
@AutoValue
public abstract class CsvExportConfig {
  public abstract ImmutableList<Column> columns();

  public static CsvExportConfig.Builder builder() {
    return new AutoValue_CsvExportConfig.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract CsvExportConfig.Builder setColumns(ImmutableList<Column> columns);

    public abstract CsvExportConfig build();
  }
}
