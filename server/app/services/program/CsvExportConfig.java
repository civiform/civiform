package services.program;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/** Contains all information needed to export a CSV file for a program. */
@AutoValue
public abstract class CsvExportConfig {
  public abstract ImmutableList<Column> columns();

  public abstract ImmutableMap<String, ImmutableList<String>> checkboxQuestionNameToOptionsMap();

  public static CsvExportConfig.Builder builder() {
    return new AutoValue_CsvExportConfig.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract CsvExportConfig.Builder setColumns(ImmutableList<Column> columns);

    public abstract CsvExportConfig.Builder setCheckboxQuestionNameToOptionsMap(
        ImmutableMap<String, ImmutableList<String>> checkboxQuestionNameToOptionsMap);

    public abstract ImmutableList.Builder<Column> columnsBuilder();

    public abstract CsvExportConfig build();

    public CsvExportConfig.Builder addColumn(Column column) {
      columnsBuilder().add(column);
      return this;
    }
  }
}
