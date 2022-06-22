package services.export;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.hash.Hashing;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
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
 *
 * <p>Call close() directly or use the try-with-resources pattern in order for the underlying {@link
 * CsvPrinter} to be closed.
 */
public final class CsvExporter implements AutoCloseable {
  private final String EMPTY_VALUE = "";

  private final ImmutableList<Column> columns;
  private final String secret;
  private final CSVPrinter printer;

  /** Provide a secret if you will need to use OPAQUE_ID type columns. */
  public CsvExporter(ImmutableList<Column> columns, String secret, Writer writer)
      throws IOException {
    this.columns = checkNotNull(columns);
    this.secret = checkNotNull(secret);

    CSVFormat format =
        CSVFormat.DEFAULT
            .builder()
            .setHeader(columns.stream().map(Column::header).toArray(String[]::new))
            .build();
    this.printer = new CSVPrinter(writer, format);
  }

  /** Writes a single {@link Application} record to the CSV. */
  public void exportRecord(
      Application application, ReadOnlyApplicantProgramService roApplicantService)
      throws IOException {
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
          if (secret.isBlank()) {
            throw new RuntimeException("Secret not present, but opaque ID requested.");
          }
          printer.print(
              application
                  .getSubmitterEmail()
                  .map(email -> opaqueIdentifier(secret, email))
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
          if (secret.isEmpty()) {
            throw new RuntimeException("Secret not present, but opaque ID requested.");
          }
          printer.print(opaqueIdentifier(secret, application.getApplicant().id));
          break;
        case APPLICANT_OPAQUE:
          if (secret.isEmpty()) {
            throw new RuntimeException("Secret not present, but opaque applicant data requested.");
          }
          // We still hash the empty value.
          printer.print(opaqueIdentifier(secret, getValueFromAnswerMap(column, answerMap)));
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

  @Override
  public void close() throws IOException {
    printer.close();
  }
}
