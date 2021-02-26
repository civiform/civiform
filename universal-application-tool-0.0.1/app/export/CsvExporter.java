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

  private void writeHeaders(CSVPrinter printer) throws IOException {
    printer.printRecord(headers);
  }

  @Override
  public void export(Applicant applicant, Writer writer) throws IOException {
    CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.withFirstRecordAsHeader());
    if (!wroteHeaders) {
      this.writeHeaders(printer);
      wroteHeaders = true;
    }
    for (String column : this.columns) {
      Optional<String> value = applicant.getApplicantData().readString(column);
      printer.print(value.orElse("COLUMN_EMPTY"));
    }
    printer.println();
  }
}
