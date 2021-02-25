package export;

import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.inject.Inject;
import models.Program;
import services.program.ExportDefinition;

public class ExporterFactory {
  @Inject
  public ExporterFactory() {
    super();
  }

  public List<Exporter> createExporters(Program program) {
    ImmutableList.Builder<Exporter> list = new ImmutableList.Builder<Exporter>();
    for (ExportDefinition e : program.getProgramDefinition().exportDefinitions()) {
      switch (e.engine()) {
        case "pdf":
          list.add(new PdfExporter());
          break;
        case "csv":
          list.add(new CsvExporter());
          break;
        default:
          throw new IllegalArgumentException(String.format("nonexistent exporter: %s", e.engine()));
      }
    }
    return list.build();
  }
}
