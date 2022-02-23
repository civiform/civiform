package services.program;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import java.util.Optional;

/** Defines how applications of a program are exported to external files. */
@AutoValue
@JsonDeserialize(builder = AutoValue_ExportDefinition.Builder.class)
public abstract class ExportDefinition {
  /** Which export engine to use. */
  @JsonProperty("engine")
  public abstract ExportEngine engine();

  /** The configuration of the PDF export - only if engine == "pdf". */
  @JsonProperty("pdfConfig")
  public abstract Optional<PdfExportConfig> pdfConfig();

  /** The configuration of the CSV export - only if engine == "csv". */
  @JsonProperty("csvConfig")
  public abstract Optional<CsvExportConfig> csvConfig();

  public static ExportDefinition.Builder builder() {
    return new AutoValue_ExportDefinition.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    @JsonProperty("engine")
    public abstract ExportDefinition.Builder setEngine(ExportEngine engine);

    @JsonProperty("csvConfig")
    public abstract ExportDefinition.Builder setCsvConfig(Optional<CsvExportConfig> csvConfig);

    @JsonProperty("pdfConfig")
    public abstract ExportDefinition.Builder setPdfConfig(Optional<PdfExportConfig> pdfConfig);

    public abstract ExportDefinition build();
  }
}
