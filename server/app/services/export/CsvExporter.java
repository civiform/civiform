package services.export;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.hash.Hashing;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import models.Application;
import models.TrustedIntermediaryGroup;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import services.Path;
import services.applicant.ReadOnlyApplicantProgramService;
import services.program.Column;

/**
 * CsvExporter takes a list of {@link Column}s and exports the data specified. A column contains a
 * {@link Path} indexing into an applicant's data, and CsvExporter takes the path and reads the
 * answer from {@link ReadOnlyApplicantProgramService} if present.
 */
public final class CsvExporter {
  private final String EMPTY_VALUE = "";

  private ImmutableList<Column> columns;
  private Optional<String> secret;
  private Optional<CSVPrinter> resultPrinter = Optional.empty();

  public CsvExporter(List<Column> columns) {
    this.columns = ImmutableList.copyOf(columns);
    this.secret = Optional.empty();
  }

  /** Provide a secret if you will need to use OPAQUE_ID type columns. */
  public CsvExporter(ImmutableList<Column> columns, String secret) {
    this(columns);
    this.secret = Optional.of(secret);
  }

  public void start(Writer writer) throws IOException {
    if (!resultPrinter.isEmpty()) {
      throw new RuntimeException("invalid state: start() should only be called once");
    }
    CSVFormat format =
        CSVFormat.DEFAULT
            .builder()
            .setHeader(columns.stream().map(Column::header).toArray(String[]::new))
            .build();
    resultPrinter = Optional.of(new CSVPrinter(writer, format));
  }

  /**
   * The CSV exporter does not store the writer between calls. Since it is intended for many
   * applications, this function is intended to be called several times.
   */
  public void exportRecord(
      Application application, ReadOnlyApplicantProgramService roApplicantService, Writer writer)
      throws IOException {
    if (resultPrinter.isEmpty()) {
      throw new RuntimeException(
          "invalid state: start() should be called prior to exporting a record");
    }

    CSVPrinter printer = resultPrinter.get();

    ImmutableMap<Path, String> answerMap =
        roApplicantService.getSummaryData().stream()
            .flatMap(data -> data.scalarAnswersInDefaultLocale().entrySet().stream())
            .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    for (Column column : columns) {
      switch (column.columnType()) {
        case APPLICANT_ANSWER:
          printer.print(getValueFromAnswerMap(column, answerMap));
          break;
        case APPLICANT_ID:
          printer.print(application.getApplicant().id);
          break;
        case APPLICATION_ID:
          printer.print(application.id);
          break;
        case LANGUAGE:
          printer.print(application.getApplicantData().preferredLocale().toLanguageTag());
          break;
        case CREATE_TIME:
          printer.print(application.getCreateTime().toString());
          break;
        case SUBMIT_TIME:
          printer.print(
              application.getSubmitTime() != null
                  ? application.getSubmitTime().toString()
                  : EMPTY_VALUE);
          break;
        case SUBMITTER_EMAIL_OPAQUE:
          if (this.secret.isEmpty()) {
            throw new RuntimeException("Secret not present, but opaque ID requested.");
          }
          printer.print(
              application
                  .getSubmitterEmail()
                  .map(email -> opaqueIdentifier(this.secret.get(), email))
                  .orElse(EMPTY_VALUE));
          break;
        case SUBMITTER_EMAIL:
          printer.print(application.getSubmitterEmail().orElse("Applicant"));
          break;
        case PROGRAM:
          printer.print(application.getProgram().getProgramDefinition().adminName());
          break;
        case TI_ORGANIZATION:
          printer.print(
              application
                  .getApplicant()
                  .getAccount()
                  .getManagedByGroup()
                  .map(TrustedIntermediaryGroup::getName)
                  .orElse(EMPTY_VALUE));
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
          // We still hash the empty value.
          printer.print(
              opaqueIdentifier(this.secret.get(), getValueFromAnswerMap(column, answerMap)));
      }
    }

    printer.println();
  }

  /**
   * Returns the answer retrieved by {@link ReadOnlyApplicantProgramService}. The value is derived
   * from the raw value in applicant data, such as translating enum number to human readable text in
   * default locale or mapping file key to download url.
   */
  private String getValueFromAnswerMap(Column column, ImmutableMap<Path, String> answerMap) {
    Path path = column.jsonPath().orElseThrow();
    if (!answerMap.containsKey(path)) {
      return EMPTY_VALUE;
    }
    return answerMap.get(path);
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
