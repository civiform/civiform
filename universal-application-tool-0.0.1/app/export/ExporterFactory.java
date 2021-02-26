package export;

import com.google.common.collect.ImmutableList;
import models.Program;
import services.program.ExportDefinition;

public class ExporterFactory {
  public ImmutableList<Exporter> createExporters(Program program) {
    ImmutableList.Builder<Exporter> list = new ImmutableList.Builder<Exporter>();
    for (ExportDefinition exportDefinition : program.getProgramDefinition().exportDefinitions()) {
      switch (exportDefinition.engine()) {
        case PDF:
          list.add(new PdfExporter());
          break;
        case CSV:
          list.add(new CsvExporter());
          break;
        default:
          throw new IllegalArgumentException(
              String.format("nonexistent exporter: %s", exportDefinition.engine()));
      }
    }
    return list.build();
  }
}
