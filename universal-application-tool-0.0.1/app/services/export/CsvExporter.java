package services.export;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import models.Application;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import services.Path;
import services.applicant.AnswerData;
import services.applicant.ReadOnlyApplicantProgramService;
import services.program.Column;

public class CsvExporter {
  private final String EMPTY_VALUE = "";

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

  protected ImmutableList<Column> getColumns() {
    return columns;
  }

  /**
   * The CSV exporter will write the headers on first call to services.export(). It does not store
   * the writer between calls. Since it is intended for many applications, this function is intended
   * to be called several times.
   */
  public void export(
      Application application, ReadOnlyApplicantProgramService roApplicantService, Writer writer)
      throws IOException {
    CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.withFirstRecordAsHeader());

    this.writeHeadersOnFirstExport(printer);

    ImmutableMap<String, AnswerData> answerMap = roApplicantService.getSummaryDataMap();
    for (Column column : getColumns()) {
      switch (column.columnType()) {
        case APPLICANT:
          String value = EMPTY_VALUE;
          String key = column.answerDataKey().orElseThrow();
          Path path = column.jsonPath().orElseThrow();
          if (answerMap.containsKey(key)) {
            ImmutableMap<Path, String> scalars = answerMap.get(key).scalarAnswersInDefaultLocale();
            if (scalars.containsKey(path)) {
              value = scalars.get(path);
            }
          }
          printer.print(value);
          break;
        case ID:
          printer.print(application.id);
          break;
        case LANGUAGE:
          printer.print(application.getApplicantData().preferredLocale().toLanguageTag());
          break;
        case SUBMIT_TIME:
          printer.print(application.getSubmitTime().toString());
          break;
        case SUBMITTER_EMAIL:
          printer.print(application.getSubmitterEmail().orElse("Applicant"));
          break;
      }
    }

    printer.println();
  }
}
