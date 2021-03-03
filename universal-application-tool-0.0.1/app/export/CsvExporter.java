package export;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Optional;
import models.Applicant;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import services.program.Column;

public class CsvExporter implements Exporter {
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
   * The CSV exporter will write the headers on first call to export(). It does not store the writer
   * between calls. Since it is intended for many applicants, this function is intended to be called
   * several times.
   */
  @Override
  public void export(Applicant applicant, Writer writer) throws IOException {
    CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.withFirstRecordAsHeader());
    this.writeHeadersOnFirstExport(printer);
    for (Column column : this.columns) {
      Optional<String> value = applicant.getApplicantData().readString(column.jsonPath());
      printer.print(value.orElse("COLUMN_EMPTY"));
    }
    printer.println();
  }
}
