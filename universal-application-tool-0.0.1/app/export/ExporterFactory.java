package export;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import models.Program;
import services.program.ExportDefinition;

public class ExporterFactory {

  public ImmutableList<Exporter> createExporters(Program program) throws IOException {
    ImmutableList.Builder<Exporter> list = new ImmutableList.Builder<Exporter>();
    for (ExportDefinition exportDefinition : program.getProgramDefinition().exportDefinitions()) {
      switch (exportDefinition.engine()) {
        case PDF:
          list.add(
              new PdfExporter(
                  exportDefinition.pdfConfig().orElseThrow().baseDocument(),
                  exportDefinition.pdfConfig().orElseThrow().mappings()));
          break;
        case CSV:
          list.add(new CsvExporter(exportDefinition.csvConfig().orElseThrow().columns()));
          break;
        default:
          throw new IllegalArgumentException(
              String.format("nonexistent exporter: %s", exportDefinition.engine()));
      }
    }
    return list.build();
  }
}
