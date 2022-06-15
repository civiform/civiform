package services.export;

import com.google.common.base.Preconditions;
import com.typesafe.config.Config;
import java.io.IOException;
import java.util.Optional;
import javax.inject.Inject;
import models.Program;
import services.program.CsvExportConfig;

// TODO(clouser): Remove this.
/** ExporterFactory helps create {@link CsvExporter} and {@link PdfExporter} objects. */
public class ExporterFactory {
  private final Config config;

  @Inject
  public ExporterFactory(Config config) {
    this.config = Preconditions.checkNotNull(config);
  }

  public CsvExporter csvExporter(Program program) throws NotConfiguredException, IOException {
    Optional<CsvExportConfig> exportConfig =
        program.getProgramDefinition().exportDefinitions().stream()
            .filter(exportDefinition -> exportDefinition.csvConfig().isPresent())
            .map(exportDefinition -> exportDefinition.csvConfig().get())
            .findAny();
    if (exportConfig.isEmpty()) {
      throw new NotConfiguredException();
    }
    return csvExporter(exportConfig.get());
  }

  public CsvExporter csvExporter(CsvExportConfig exportConfig) throws IOException {
    return new CsvExporter(exportConfig.columns(), config.getString("play.http.secret.key"), null);
  }
}
