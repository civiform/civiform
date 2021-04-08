package services.export;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Optional;
import models.Application;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import services.program.Column;

public class CsvExporter {
  private boolean wroteHeaders;
  private ImmutableList<Column> columns;

  public CsvExporter(List<Column> columns) {
    this.wroteHeaders = false;
    this.columns = ImmutableList.copyOf(columns);
  }

  private void writeHeadersOnFirstExport(CSVPrinter printer) throws IOException {
    if (!wroteHeaders) {
      for (Column column : columns) {
        printer.print(column.header());
      }
      printer.println();
      wroteHeaders = true;
    }
  }

  /**
   * The CSV exporter will write the headers on first call to services.export(). It does not store
   * the writer between calls. Since it is intended for many applications, this function is intended
   * to be called several times.
   */
  public void export(Application application, Writer writer) throws IOException {
    CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.withFirstRecordAsHeader());
    this.writeHeadersOnFirstExport(printer);
    for (Column column : this.columns) {
      switch (column.columnType()) {
        case APPLICANT:
          Optional<String> value =
              application.getApplicantData().readAsString(column.jsonPath().orElseThrow());
          printer.print(value.orElse("COLUMN_EMPTY"));
          break;
        case ID:
          printer.print(application.id);
          break;
        case SUBMIT_TIME:
          printer.print(application.getSubmitTime().toString());
          break;
      }
    }
    printer.println();
  }
}
