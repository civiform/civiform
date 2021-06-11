package services.export;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.hash.Hashing;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import models.Application;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import services.Path;
import services.applicant.AnswerData;
import services.applicant.ApplicantData;
import services.applicant.ReadOnlyApplicantProgramService;
import services.program.Column;

public class CsvExporter {
  private final String EMPTY_VALUE = "";

  private boolean exportOneParticularProgram;
  private boolean wroteHeaders;
  private ImmutableList<Column> columns;
  private Optional<String> secret;

  public CsvExporter(List<Column> columns, boolean exportOneProgram) {
    this.exportOneParticularProgram = exportOneProgram;
    this.wroteHeaders = false;
    this.columns = ImmutableList.copyOf(columns);
    this.secret = Optional.empty();
  }

  /** Provide a secret if you will need to use OPAQUE_ID type columns. */
  public CsvExporter(List<Column> columns, boolean exportOneProgram, String secret) {
    this(columns, exportOneProgram);
    this.secret = Optional.of(secret);
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
          if (exportOneParticularProgram) {
            value = getValueFromAnswerMap(column, answerMap);
          } else {
            value = getValueFromApplicantData(column, application.getApplicantData());
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
        case OPAQUE_ID:
          if (this.secret.isEmpty()) {
            throw new RuntimeException("Secret not present, but opaque ID requested.");
          }
          printer.print(opaqueIdentifier(this.secret.get(), application.getApplicant().id));
          break;
        case APPLICANT_OPAQUE:
          if (this.secret.isEmpty()) {
            throw new RuntimeException("Secret not present, but opaque applicant data requested.");
          }
          Optional<String> applicantValue =
              application.getApplicantData().readAsString(column.jsonPath().orElseThrow());
          // We still hash the empty value.
          printer.print(opaqueIdentifier(this.secret.get(), applicantValue.orElse(EMPTY_VALUE)));
      }
    }

    printer.println();
  }

  /**
   * Returns the answer retrieved by {@link ReadOnlyApplicantProgramService}. The value is derived
   * from the raw value in applicant data, such as translating enum number to human readable text in
   * default locale or mapping file key to download url.
   */
  private String getValueFromAnswerMap(Column column, ImmutableMap<String, AnswerData> answerMap) {
    String key = column.answerDataKey().orElseThrow();
    Path path = column.jsonPath().orElseThrow();
    if (!answerMap.containsKey(key)) {
      return EMPTY_VALUE;
    }
    ImmutableMap<Path, String> scalars = answerMap.get(key).scalarAnswersInDefaultLocale();
    if (!scalars.containsKey(path)) {
      return EMPTY_VALUE;
    }
    return scalars.get(path);
  }

  /** Returns the raw value in applicant data. */
  private String getValueFromApplicantData(Column column, ApplicantData applicantData) {
    return applicantData.readAsString(column.jsonPath().orElseThrow()).orElse(EMPTY_VALUE);
  }

  /** Returns an opaque identifier - the ID hashed with the application secret key. */
  private static String opaqueIdentifier(String secret, Long id) {
    return Hashing.sha256()
        .newHasher()
        .putString(secret, StandardCharsets.UTF_8)
        .putLong(id)
        .hash()
        .toString();
  }

  private static String opaqueIdentifier(String secret, String value) {
    return Hashing.sha256()
        .newHasher()
        .putString(secret, StandardCharsets.UTF_8)
        .putString(value, StandardCharsets.UTF_8)
        .hash()
        .toString();
  }
}
