package services.program;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

/** Contains all information needed to export a CSV file for a program. */
@AutoValue
@JsonDeserialize(builder = AutoValue_CsvExportConfig.Builder.class)
public abstract class CsvExportConfig {
  @JsonProperty("columns")
  public abstract ImmutableList<Column> columns();

  public static CsvExportConfig.Builder builder() {
    return new AutoValue_CsvExportConfig.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    @JsonProperty("columns")
    public abstract CsvExportConfig.Builder setColumns(ImmutableList<Column> columns);

    public abstract ImmutableList.Builder<Column> columnsBuilder();

    public abstract CsvExportConfig build();

    public CsvExportConfig.Builder addColumn(Column column) {
      columnsBuilder().add(column);
      return this;
    }
  }
}
