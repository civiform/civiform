package export;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Optional;
import models.Applicant;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

public class CsvExporter implements Exporter {
  private boolean wroteHeaders;
  private List<String> headers;
  private List<String> columns;

  public CsvExporter(List<String> headers, List<String> columns) {
    this.wroteHeaders = false;
    this.headers = headers;
    this.columns = columns;
  }

  private void maybeWriteHeaders(CSVPrinter printer) throws IOException {
    if (!wroteHeaders) {
      printer.printRecord(headers);
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
    this.maybeWriteHeaders(printer);
    for (String column : this.columns) {
      Optional<String> value = applicant.getApplicantData().readString(column);
      printer.print(value.orElse("COLUMN_EMPTY"));
    }
    printer.println();
  }
}
