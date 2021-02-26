package services.program;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

@AutoValue
@JsonDeserialize(builder = AutoValue_CsvExportConfig.Builder.class)
public abstract class CsvExportConfig {
    public abstract ImmutableList<String> headers();
    public abstract ImmutableList<String> columns();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract CsvExportConfig.Builder setHeaders(ImmutableList<String> headers);
        public abstract CsvExportConfig.Builder setColumns(ImmutableList<String> headers);
        public abstract ImmutableList.Builder<String> headersBuilder();
        public abstract ImmutableList.Builder<String> columnsBuilder();
        public abstract CsvExportConfig build();
        public CsvExportConfig.Builder addHeader(String header) {
            headersBuilder().add(header);
            return this;
        }

        public CsvExportConfig.Builder addColumn(String column) {
            columnsBuilder().add(column);
            return this;
        }
    }
}
