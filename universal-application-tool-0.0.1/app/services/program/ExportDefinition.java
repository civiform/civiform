package services.program;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import javax.annotation.Nullable;

@AutoValue
@JsonDeserialize(builder = AutoValue_ExportDefinition.Builder.class)
public abstract class ExportDefinition {
  @JsonProperty("engine")
  /** Which export engine to use. */
  public abstract String engine();

  @JsonProperty("pdfConfig")
  @Nullable
  /** The configuration of the PDF export - only if engine == "pdf". */
  public abstract PdfExportConfig pdfConfig();

  @JsonProperty("csvConfig")
  @Nullable
  /** The configuration of the CSV export - only if engine == "csv". */
  public abstract CsvExportConfig csvConfig();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract ExportDefinition.Builder setEngine(String engine);

    public abstract ExportDefinition.Builder setCsvConfig(CsvExportConfig csvConfig);

    public abstract ExportDefinition.Builder setPdfConfig(PdfExportConfig pdfConfig);

    public abstract ExportDefinition build();
  }
}
