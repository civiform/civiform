package services.export;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import repository.ExportServiceRepository;
import services.Path;
import services.applicant.ApplicantData;
import services.applicant.Currency;
import services.applicant.question.AddressQuestion;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.CurrencyQuestion;
import services.applicant.question.DateQuestion;
import services.applicant.question.EmailQuestion;
import services.applicant.question.FileUploadQuestion;
import services.applicant.question.IdQuestion;
import services.applicant.question.MultiSelectQuestion;
import services.applicant.question.NameQuestion;
import services.applicant.question.NumberQuestion;
import services.applicant.question.PhoneQuestion;
import services.applicant.question.Scalar;
import services.applicant.question.SingleSelectQuestion;
import services.applicant.question.TextQuestion;
import services.export.enums.ColumnType;
import services.export.enums.MultiOptionSelectionExportType;
import services.question.LocalizedQuestionOption;
import services.question.types.ScalarType;
import services.settings.SettingsManifest;

final class CsvColumnFactory {
  private static final String HEADER_SPACER_ENUM = " - ";
  private static final String HEADER_SPACER_SCALAR = " ";
  private static final String CURRENCY_CENTS_TYPE_STRING =
      ScalarType.CURRENCY_CENTS.toString().toLowerCase(Locale.ROOT);
  private static final String FILE_KEY_LIST =
      Scalar.FILE_KEY_LIST.toString().toLowerCase(Locale.ROOT);
  private static final String NAME_SUFFIX = Scalar.NAME_SUFFIX.toString().toLowerCase(Locale.ROOT);
  private static final String SERVICE_AREA =
      Scalar.SERVICE_AREA.toString().toLowerCase(Locale.ROOT);
  private static final String SERVICE_AREAS =
      Scalar.SERVICE_AREAS.toString().toLowerCase(Locale.ROOT);

  private final SettingsManifest settingsManifest;
  private final ExportServiceRepository exportServiceRepository;

  @Inject
  CsvColumnFactory(
      SettingsManifest settingsManifest, ExportServiceRepository exportServiceRepository) {
    this.settingsManifest = checkNotNull(settingsManifest);
    this.exportServiceRepository = checkNotNull(exportServiceRepository);
  }

  Stream<Column> buildColumns(
      ApplicantQuestion aq, ColumnType columnType, boolean isMultipleFileUploadEnabled) {
    switch (aq.getType()) {
      case ADDRESS:
        return buildColumnsForAddressQuestion(aq.createAddressQuestion(), columnType);
      case CHECKBOX:
        return buildColumnsForMultiSelectQuestion(aq.createMultiSelectQuestion(), columnType);
      case CURRENCY:
        return buildColumnsForCurrencyQuestion(aq.createCurrencyQuestion(), columnType);
      case DATE:
        return buildColumnsForDateQuestion(aq.createDateQuestion(), columnType);
      case DROPDOWN:
      case RADIO_BUTTON:
        return buildColumnsForSingleSelectQuestion(aq.createSingleSelectQuestion(), columnType);
      case EMAIL:
        return buildColumnsForEmailQuestion(aq.createEmailQuestion(), columnType);
        // Enumerator questions themselves are not included in the CSV, but their repeated questions
        // are.
      case ENUMERATOR:
        return Stream.empty();
      case FILEUPLOAD:
        return buildColumnsForFileUploadQuestion(
            aq.createFileUploadQuestion(), columnType, isMultipleFileUploadEnabled);
      case ID:
        return buildColumnsForIdQuestion(aq.createIdQuestion(), columnType);
      case NAME:
        return buildColumnsForNameQuestion(aq.createNameQuestion(), columnType);
      case NUMBER:
        return buildColumnsForNumberQuestion(aq.createNumberQuestion(), columnType);
      case PHONE:
        return buildColumnsForPhoneQuestion(aq.createPhoneQuestion(), columnType);
        // Static content questions are not included in CSV because they do not include an answer
        // from the user.
      case STATIC:
        return Stream.empty();
      case TEXT:
        return buildColumnsForTextQuestion(aq.createTextQuestion(), columnType);
      default:
        throw new RuntimeException(String.format("Unrecognized questionType %s", aq.getType()));
    }
  }

  private Stream<Column> buildColumnsForAddressQuestion(AddressQuestion q, ColumnType columnType) {
    return Stream.of(
        // Primary columns
        Column.builder()
            .setColumnType(columnType)
            .setHeader(formatHeader(q.getStreetPath()))
            .setQuestionPath(q.getContextualizedPath())
            .setAnswerExtractor(aq -> ((AddressQuestion) aq).getStreetValue().orElse(""))
            .build(),
        Column.builder()
            .setColumnType(columnType)
            .setHeader(formatHeader(q.getLine2Path()))
            .setQuestionPath(q.getContextualizedPath())
            .setAnswerExtractor(aq -> ((AddressQuestion) aq).getLine2Value().orElse(""))
            .build(),
        Column.builder()
            .setColumnType(columnType)
            .setHeader(formatHeader(q.getCityPath()))
            .setQuestionPath(q.getContextualizedPath())
            .setAnswerExtractor(aq -> ((AddressQuestion) aq).getCityValue().orElse(""))
            .build(),
        Column.builder()
            .setColumnType(columnType)
            .setHeader(formatHeader(q.getStatePath()))
            .setQuestionPath(q.getContextualizedPath())
            .setAnswerExtractor(aq -> ((AddressQuestion) aq).getStateValue().orElse(""))
            .build(),
        Column.builder()
            .setColumnType(columnType)
            .setHeader(formatHeader(q.getZipPath()))
            .setQuestionPath(q.getContextualizedPath())
            .setAnswerExtractor(aq -> ((AddressQuestion) aq).getZipValue().orElse(""))
            .build(),
        // Address correction columns
        Column.builder()
            .setColumnType(columnType)
            .setHeader(formatHeader(q.getCorrectedPath()))
            .setQuestionPath(q.getContextualizedPath())
            .setAnswerExtractor(aq -> ((AddressQuestion) aq).getCorrectedValue().orElse(""))
            .build(),
        Column.builder()
            .setColumnType(columnType)
            .setHeader(formatHeader(q.getLatitudePath()))
            .setQuestionPath(q.getContextualizedPath())
            .setAnswerExtractor(
                aq ->
                    ((AddressQuestion) aq)
                        .getLatitudeValue()
                        .map(ExportFormatUtils::formatLatOrLongAsString)
                        .orElse(""))
            .build(),
        Column.builder()
            .setColumnType(columnType)
            .setHeader(formatHeader(q.getLongitudePath()))
            .setQuestionPath(q.getContextualizedPath())
            .setAnswerExtractor(
                aq ->
                    ((AddressQuestion) aq)
                        .getLongitudeValue()
                        .map(ExportFormatUtils::formatLatOrLongAsString)
                        .orElse(""))
            .build(),
        Column.builder()
            .setColumnType(columnType)
            .setHeader(formatHeader(q.getWellKnownIdPath()))
            .setQuestionPath(q.getContextualizedPath())
            .setAnswerExtractor(
                aq ->
                    ((AddressQuestion) aq)
                        .getWellKnownIdValue()
                        .map(w -> Long.toString(w))
                        .orElse(""))
            .build(),
        Column.builder()
            .setColumnType(columnType)
            .setHeader(formatHeader(q.getServiceAreasPath()))
            .setQuestionPath(q.getContextualizedPath())
            .setAnswerExtractor(
                aq ->
                    ((AddressQuestion) aq)
                        .getServiceAreaValue()
                        .map(ExportFormatUtils::serializeServiceArea)
                        .orElse(""))
            .build());
  }

  private Stream<Column> buildColumnsForCurrencyQuestion(
      CurrencyQuestion q, ColumnType columnType) {
    return Stream.of(
        Column.builder()
            .setColumnType(columnType)
            .setHeader(formatHeader(q.getCurrencyPath()))
            .setQuestionPath(q.getContextualizedPath())
            .setAnswerExtractor(
                cq ->
                    ((CurrencyQuestion) cq)
                        .getCurrencyValue()
                        .map(Currency::getDollarsString)
                        .orElse(""))
            .build());
  }

  private Stream<Column> buildColumnsForDateQuestion(DateQuestion q, ColumnType columnType) {
    return Stream.of(
        Column.builder()
            .setColumnType(columnType)
            .setHeader(formatHeader(q.getDatePath()))
            .setQuestionPath(q.getContextualizedPath())
            .setAnswerExtractor(
                dq ->
                    ((DateQuestion) dq)
                        .getDateValue()
                        .map(
                            localDate ->
                                localDate.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")))
                        .orElse(""))
            .build());
  }

  private Stream<Column> buildColumnsForEmailQuestion(EmailQuestion q, ColumnType columnType) {
    return Stream.of(
        Column.builder()
            .setColumnType(columnType)
            .setHeader(formatHeader(q.getEmailPath()))
            .setQuestionPath(q.getContextualizedPath())
            .setAnswerExtractor(eq -> ((EmailQuestion) eq).getEmailValue().orElse(""))
            .build());
  }

  private Stream<Column> buildColumnsForFileUploadQuestion(
      FileUploadQuestion q, ColumnType columnType, boolean isMultipleFileUploadEnabled) {
    String baseUrl = settingsManifest.getBaseUrl().orElse("");

    Column fileKeyListColumn =
        Column.builder()
            .setColumnType(columnType)
            .setHeader(formatHeader(q.getFileKeyListPath()))
            .setQuestionPath(q.getContextualizedPath())
            .setAnswerExtractor(
                fuq -> {
                  FileUploadQuestion innerQ = (FileUploadQuestion) fuq;

                  Stream<String> keyStream =
                      isMultipleFileUploadEnabled
                          ? innerQ
                              .getFileKeyListValue()
                              .map(ImmutableList::stream)
                              .orElseGet(Stream::empty)
                          : innerQ.getFileKeyValue().stream();

                  return keyStream
                      .map(fileKey -> ExportFormatUtils.formatFileUrlForAdmin(baseUrl, fileKey))
                      .collect(Collectors.joining(", "));
                })
            .build();

    if (isMultipleFileUploadEnabled) {
      return Stream.of(fileKeyListColumn);
    }

    Column fileKeyColumn =
        Column.builder()
            .setColumnType(columnType)
            .setHeader(formatHeader(q.getFileKeyPath()))
            .setQuestionPath(q.getContextualizedPath())
            .setAnswerExtractor(
                fuq ->
                    ((FileUploadQuestion) fuq)
                        .getFileKeyValue()
                        .map(fileKey -> ExportFormatUtils.formatFileUrlForAdmin(baseUrl, fileKey))
                        .orElse(""))
            .build();

    return Stream.of(fileKeyColumn, fileKeyListColumn);
  }

  private Stream<Column> buildColumnsForIdQuestion(IdQuestion q, ColumnType columnType) {
    return Stream.of(
        Column.builder()
            .setColumnType(columnType)
            .setHeader(formatHeader(q.getIdPath()))
            .setQuestionPath(q.getContextualizedPath())
            .setAnswerExtractor(iq -> ((IdQuestion) iq).getIdValue().orElse(""))
            .build());
  }

  private Stream<Column> buildColumnsForMultiSelectQuestion(
      MultiSelectQuestion q, ColumnType columnType) {
    // We only build columns once per unique contextualized question path, so for regular questions
    // this query should only be run once per question.
    // For a repeated multi-select question, which has a unique contextualized path for each
    // repeated entity, it will be called n times, where n is the largest number of repeated
    // entities with this question of all applications in the export set.
    // To fix this we could add a short-lived cache to store the options for each multi-option
    // question, but it should only last for the lifecycle of the export request to avoid it getting
    // stale when the multi-select question is modified.
    return exportServiceRepository
        .getAllHistoricMultiOptionAdminNames(q.getQuestionDefinition())
        .stream()
        .map(
            option ->
                Column.builder()
                    .setColumnType(columnType)
                    .setHeader(CsvColumnFactory.formatHeader(q.getSelectionPath(), option))
                    .setQuestionPath(q.getContextualizedPath())
                    .setAnswerExtractor(
                        msq ->
                            getMultiSelectQuestionAnswerForCsv((MultiSelectQuestion) msq, option))
                    .build());
  }

  private String getMultiSelectQuestionAnswerForCsv(MultiSelectQuestion q, String option) {
    if (!q.isAnswered()) {
      return MultiOptionSelectionExportType.NOT_ANSWERED.toString();
    }

    ImmutableList<String> selectedList = q.getSelectedOptionAdminNames().orElse(ImmutableList.of());

    ImmutableList<String> allOptionsShownInQuestionVersion =
        q.getOptions().stream()
            .map(LocalizedQuestionOption::adminName)
            .collect(ImmutableList.toImmutableList());

    if (!allOptionsShownInQuestionVersion.contains(option)) {
      return MultiOptionSelectionExportType.NOT_AN_OPTION_AT_PROGRAM_VERSION.toString();
    }

    if (selectedList.contains(option)) {
      return MultiOptionSelectionExportType.SELECTED.toString();
    }

    return MultiOptionSelectionExportType.NOT_SELECTED.toString();
  }

  private Stream<Column> buildColumnsForNameQuestion(NameQuestion q, ColumnType columnType) {
    return Stream.of(
        Column.builder()
            .setColumnType(columnType)
            .setHeader(formatHeader(q.getFirstNamePath()))
            .setQuestionPath(q.getContextualizedPath())
            .setAnswerExtractor(nq -> ((NameQuestion) nq).getFirstNameValue().orElse(""))
            .build(),
        Column.builder()
            .setColumnType(columnType)
            .setHeader(formatHeader(q.getMiddleNamePath()))
            .setQuestionPath(q.getContextualizedPath())
            .setAnswerExtractor(nq -> ((NameQuestion) nq).getMiddleNameValue().orElse(""))
            .build(),
        Column.builder()
            .setColumnType(columnType)
            .setHeader(formatHeader(q.getLastNamePath()))
            .setQuestionPath(q.getContextualizedPath())
            .setAnswerExtractor(nq -> ((NameQuestion) nq).getLastNameValue().orElse(""))
            .build(),
        Column.builder()
            .setColumnType(columnType)
            .setHeader(formatHeader(q.getNameSuffixPath()))
            .setQuestionPath(q.getContextualizedPath())
            .setAnswerExtractor(nq -> ((NameQuestion) nq).getNameSuffixValue().orElse(""))
            .build());
  }

  private Stream<Column> buildColumnsForNumberQuestion(NumberQuestion q, ColumnType columnType) {
    return Stream.of(
        Column.builder()
            .setColumnType(columnType)
            .setHeader(formatHeader(q.getNumberPath()))
            .setQuestionPath(q.getContextualizedPath())
            .setAnswerExtractor(
                nq ->
                    ((NumberQuestion) nq)
                        .getNumberValue()
                        .map(ExportFormatUtils::formatNumberAnswer)
                        .orElse(""))
            .build());
  }

  private Stream<Column> buildColumnsForPhoneQuestion(PhoneQuestion q, ColumnType columnType) {
    return Stream.of(
        Column.builder()
            .setColumnType(columnType)
            .setHeader(formatHeader(q.getPhoneNumberPath()))
            .setQuestionPath(q.getContextualizedPath())
            .setAnswerExtractor(pq -> ((PhoneQuestion) pq).getPhoneNumberValue().orElse(""))
            .build(),
        Column.builder()
            .setColumnType(columnType)
            .setHeader(formatHeader(q.getCountryCodePath()))
            .setQuestionPath(q.getContextualizedPath())
            .setAnswerExtractor(pq -> ((PhoneQuestion) pq).getCountryCodeValue().orElse(""))
            .build());
  }

  private Stream<Column> buildColumnsForSingleSelectQuestion(
      SingleSelectQuestion q, ColumnType columnType) {
    return Stream.of(
        Column.builder()
            .setColumnType(columnType)
            .setHeader(formatHeader(q.getSelectionPath()))
            .setQuestionPath(q.getContextualizedPath())
            .setAnswerExtractor(
                ssq -> ((SingleSelectQuestion) ssq).getSelectedOptionAdminName().orElse(""))
            .build());
  }

  private Stream<Column> buildColumnsForTextQuestion(TextQuestion q, ColumnType columnType) {
    return Stream.of(
        Column.builder()
            .setColumnType(columnType)
            .setHeader(formatHeader(q.getTextPath()))
            .setQuestionPath(q.getContextualizedPath())
            .setAnswerExtractor(tq -> ((TextQuestion) tq).getTextValue().orElse(""))
            .build());
  }

  /**
   * Convert {@link Path} to a human-readable header string.
   *
   * <p>The {@link ApplicantData#APPLICANT_PATH} is ignored, enumerator references are separated by
   * {@link #HEADER_SPACER_ENUM} and the scalar is separated by {@link #HEADER_SPACER_SCALAR}.
   *
   * <p>Example: "applicant.household_members[3].household_member_name.first_name" becomes
   * "household members[3] - household member name (first_name)"
   *
   * <p>The currency_cents scalar is special cased to be named currency as the data will be dollars.
   *
   * @param scalarPath is a path that ends in a {@link services.applicant.question.Scalar}
   * @param optionAdminName the admin name of the multi-option question option, if it's a
   *     multi-option question
   * @return the String to use as the column header.
   */
  private static String formatHeader(Path scalarPath, String optionAdminName) {
    return formatHeader(scalarPath, Optional.of(optionAdminName));
  }

  private static String formatHeader(Path scalarPath) {
    return formatHeader(scalarPath, Optional.empty());
  }

  private static String formatHeader(Path scalarPath, Optional<String> optionAdminName) {
    Path finalPath = scalarPath;
    String scalarComponent =
        optionAdminName
            .map(o -> String.format("(%s - %s)", finalPath.keyName(), o))
            .orElse(String.format("(%s)", finalPath.keyName()));
    // Remove "cents" from the currency string as the value will be dollars.
    if (scalarPath.keyName().equals(CURRENCY_CENTS_TYPE_STRING)) {
      scalarComponent = "(currency)";
    }

    // Remove "name" from the name suffix string as it will be indicated in the name.
    if (scalarPath.keyName().equals(NAME_SUFFIX)) {
      scalarComponent = "(suffix)";
    }

    // Change scalar name for file_key_list
    if (scalarPath.keyName().equals(FILE_KEY_LIST)) {
      scalarComponent = "(file_urls)";
    }

    // TODO: #7134 Only here for backwards compatibility. Long term this should go away
    if (scalarPath.keyName().equals(SERVICE_AREAS)) {
      scalarComponent = String.format("(%s)", SERVICE_AREA);
    }

    List<String> reversedHeaderComponents = new ArrayList<>(Arrays.asList(scalarComponent));
    while (!scalarPath.parentPath().isEmpty()
        && !scalarPath.parentPath().equals(ApplicantData.APPLICANT_PATH)) {
      scalarPath = scalarPath.parentPath();
      String headerComponent = scalarPath.keyName().replace("_", " ");
      reversedHeaderComponents.add(headerComponent);
    }

    // The pieces to the header are built in reverse, as we reference path#parentPath(), so we build
    // the header string going backwards through the list.
    StringBuilder builder = new StringBuilder();
    for (int i = reversedHeaderComponents.size() - 1; i >= 0; i--) {
      builder.append(reversedHeaderComponents.get(i));
      if (i > 1) {
        builder.append(HEADER_SPACER_ENUM);
      } else if (i == 1) {
        builder.append(HEADER_SPACER_SCALAR);
      }
    }
    return builder.toString();
  }
}
