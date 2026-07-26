package services.export;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import repository.ExportServiceRepository;
import services.Path;
import services.applicant.ApplicantData;
import services.applicant.ApplicationScoreMetadata;
import services.applicant.Currency;
import services.applicant.question.AddressQuestion;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.CurrencyQuestion;
import services.applicant.question.DateQuestion;
import services.applicant.question.EmailQuestion;
import services.applicant.question.FileUploadQuestion;
import services.applicant.question.IdQuestion;
import services.applicant.question.MapQuestion;
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
import services.question.QuestionOption;
import services.question.types.QuestionType;
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

  Stream<Column> buildColumns(ApplicantQuestion aq, ColumnType columnType) {
    return buildColumns(aq, columnType, /* includeScoreColumn= */ false);
  }

  /**
   * Builds the columns for a question. When {@code includeScoreColumn} is true (only for questions
   * that qualified for a score column: scoring flag on, a represented program version uses scoring,
   * and the question type supports option scores), the question's own column stream additionally
   * contains its score column so adjacency is structural.
   */
  Stream<Column> buildColumns(ApplicantQuestion aq, ColumnType columnType, boolean includeScoreColumn) {
    // Yes/No questions share the single-select branch but never score.
    boolean scoreColumn = includeScoreColumn && QuestionType.supportsOptionScores(aq.getType());
    return switch (aq.getType()) {
      case ADDRESS -> buildColumnsForAddressQuestion(aq.createAddressQuestion(), columnType);
      case CHECKBOX ->
          buildColumnsForMultiSelectQuestion(aq.createMultiSelectQuestion(), columnType, scoreColumn);
      case CURRENCY -> buildColumnsForCurrencyQuestion(aq.createCurrencyQuestion(), columnType);
      case DATE -> buildColumnsForDateQuestion(aq.createDateQuestion(), columnType);
      case DROPDOWN, RADIO_BUTTON, YES_NO ->
          buildColumnsForSingleSelectQuestion(aq.createSingleSelectQuestion(), columnType, scoreColumn);
      case EMAIL -> buildColumnsForEmailQuestion(aq.createEmailQuestion(), columnType);
        // Enumerator questions themselves are not included in the CSV, but their repeated questions
        // are.
      case ENUMERATOR -> Stream.empty();
      case FILEUPLOAD ->
          buildColumnsForFileUploadQuestion(aq.createFileUploadQuestion(), columnType);
      case ID -> buildColumnsForIdQuestion(aq.createIdQuestion(), columnType);
      case MAP -> buildColumnsForMapQuestion(aq.createMapQuestion(), columnType);
      case NAME -> buildColumnsForNameQuestion(aq.createNameQuestion(), columnType);
      case NUMBER -> buildColumnsForNumberQuestion(aq.createNumberQuestion(), columnType);
      case PHONE -> buildColumnsForPhoneQuestion(aq.createPhoneQuestion(), columnType);
        // Static content questions are not included in CSV because they do not include an answer
        // from the user.
      case STATIC -> Stream.empty();
      case TEXT -> buildColumnsForTextQuestion(aq.createTextQuestion(), columnType);
      default ->
          throw new RuntimeException(String.format("Unrecognized questionType %s", aq.getType()));
    };
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
      FileUploadQuestion q, ColumnType columnType) {
    String baseUrl = settingsManifest.getBaseUrl().orElse("");

    Column fileKeyListColumn =
        Column.builder()
            .setColumnType(columnType)
            .setHeader(formatHeader(q.getFileKeyListPath()))
            .setQuestionPath(q.getContextualizedPath())
            .setAnswerExtractor(
                fuq ->
                    ((FileUploadQuestion) fuq)
                        .getFileKeyListValue()
                        .map(ImmutableList::stream)
                        .orElseGet(Stream::empty)
                        .map(fileKey -> ExportFormatUtils.formatFileUrlForAdmin(baseUrl, fileKey))
                        .collect(Collectors.joining(", ")))
            .build();

    return Stream.of(fileKeyListColumn);
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
      MultiSelectQuestion q, ColumnType columnType, boolean includeScoreColumn) {
    // We only build columns once per unique contextualized question path, so for regular questions
    // this query should only be run once per question.
    // For a repeated multi-select question, which has a unique contextualized path for each
    // repeated entity, it will be called n times, where n is the largest number of repeated
    // entities with this question of all applications in the export set.
    // To fix this we could add a short-lived cache to store the options for each multi-option
    // question, but it should only last for the lifecycle of the export request to avoid it getting
    // stale when the multi-select question is modified.
    Stream<Column> optionColumns =
        exportServiceRepository
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
                                getMultiSelectQuestionAnswerForCsv(
                                    (MultiSelectQuestion) msq, option))
                        .build());
    if (!includeScoreColumn) {
      return optionColumns;
    }
    // The score column comes once, after all per-option columns.
    return Stream.concat(
        optionColumns, Stream.of(buildMultiSelectScoreColumn(q, columnType)));
  }

  private static Column buildMultiSelectScoreColumn(MultiSelectQuestion q, ColumnType columnType) {
    // The header join produces the same sibling path ApplicationScoreMetadata derives for
    // storage; values are still read through the helper.
    return Column.builder()
        .setColumnType(columnType)
        .setHeader(formatHeader(q.getContextualizedPath().join("score")))
        .setQuestionPath(q.getContextualizedPath())
        .setAnswerExtractor(
            msq -> {
              // Read the row-specific persisted snapshot through the row's own question; never
              // capture the exemplar question used to construct the column.
              MultiSelectQuestion question = (MultiSelectQuestion) msq;
              ApplicantData data = question.getApplicantQuestion().getApplicantData();
              if (data.readDouble(ApplicationScoreMetadata.totalScorePath()).isEmpty()) {
                // Scoring was not applied to this application (flag-off era, program off, or
                // pre-feature).
                return "";
              }
              Path contextualizedPath = question.getApplicantQuestion().getContextualizedPath();
              Optional<ImmutableList<Long>> selections =
                  data.readLongList(contextualizedPath.join(Scalar.SELECTIONS));
              Optional<List<Double>> scores =
                  data.readNullableDoubleList(
                      ApplicationScoreMetadata.scoresPath(contextualizedPath));
              if (selections.isEmpty() || scores.isEmpty()) {
                // Unanswered question.
                return "";
              }
              if (selections.get().size() != scores.get().size()) {
                // Corrupt metadata; render blank rather than mispairing.
                return "";
              }
              List<Double> contributing =
                  scores.get().stream().filter(Objects::nonNull).collect(Collectors.toList());
              if (contributing.isEmpty()) {
                // All-null scores (no selected option was scored) render blank; an explicit 0 or
                // a cancel-to-zero sum renders 0 below.
                return "";
              }
              // Exact decimal arithmetic over the persisted values, so sums of admin-entered
              // decimals carry no binary floating-point artifacts.
              BigDecimal sum =
                  contributing.stream()
                      .map(BigDecimal::valueOf)
                      .reduce(BigDecimal.ZERO, BigDecimal::add);
              return QuestionOption.formatScore(sum.doubleValue());
            })
        .build();
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
      SingleSelectQuestion q, ColumnType columnType, boolean includeScoreColumn) {
    Column selectionColumn =
        Column.builder()
            .setColumnType(columnType)
            .setHeader(formatHeader(q.getSelectionPath()))
            .setQuestionPath(q.getContextualizedPath())
            .setAnswerExtractor(
                ssq -> ((SingleSelectQuestion) ssq).getSelectedOptionAdminName().orElse(""))
            .build();
    if (!includeScoreColumn) {
      return Stream.of(selectionColumn);
    }
    // The score column comes immediately after the (selection) column.
    return Stream.of(selectionColumn, buildSingleSelectScoreColumn(q, columnType));
  }

  private static Column buildSingleSelectScoreColumn(
      SingleSelectQuestion q, ColumnType columnType) {
    // The header join produces the same sibling path ApplicationScoreMetadata derives for
    // storage; values are still read through the helper.
    return Column.builder()
        .setColumnType(columnType)
        .setHeader(formatHeader(q.getContextualizedPath().join("score")))
        .setQuestionPath(q.getContextualizedPath())
        .setAnswerExtractor(
            ssq -> {
              // Read the row-specific persisted snapshot through the row's own question; never
              // capture the exemplar question used to construct the column.
              SingleSelectQuestion question = (SingleSelectQuestion) ssq;
              ApplicantData data = question.getApplicantQuestion().getApplicantData();
              if (data.readDouble(ApplicationScoreMetadata.totalScorePath()).isEmpty()) {
                // Scoring was not applied to this application (flag-off era, program off, or
                // pre-feature).
                return "";
              }
              // Blank when the question is unanswered or the selected option is unscored.
              return data
                  .readDouble(
                      ApplicationScoreMetadata.scorePath(
                          question.getApplicantQuestion().getContextualizedPath()))
                  .map(QuestionOption::formatScore)
                  .orElse("");
            })
        .build();
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

  private Stream<Column> buildColumnsForMapQuestion(MapQuestion q, ColumnType columnType) {
    return Stream.of(
        Column.builder()
            .setColumnType(columnType)
            .setHeader(formatHeader(q.getSelectionPath()))
            .setQuestionPath(q.getContextualizedPath())
            .setAnswerExtractor(mq -> String.valueOf(((MapQuestion) mq).getSelectedLocationIds()))
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
    return formatHeader(scalarPath, /* optionAdminName= */ Optional.empty());
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
