package services.export;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.typesafe.config.Config;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.inject.Inject;
import models.ApplicationModel;
import models.QuestionTag;
import play.libs.F;
import repository.ExportServiceRepository;
import repository.SubmittedApplicationFilter;
import repository.TimeFilter;
import services.DateConverter;
import services.IdentifierBasedPaginationSpec;
import services.Path;
import services.applicant.ApplicantData;
import services.applicant.ApplicantService;
import services.applicant.ReadOnlyApplicantProgramService;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.Scalar;
import services.program.Column;
import services.program.ColumnType;
import services.program.CsvExportConfig;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramQuestionDefinition;
import services.program.ProgramService;
import services.question.QuestionService;
import services.question.types.QuestionType;
import services.question.types.ScalarType;

/**
 * ExporterService generates CSV files for applications to a program or demographic information
 * across all programs.
 */
public final class CsvExporterService {

  private final ProgramService programService;
  private final QuestionService questionService;
  private final ApplicantService applicantService;
  private final Config config;
  private final DateConverter dateConverter;

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

  public static final ImmutableSet<QuestionType> NON_EXPORTED_QUESTION_TYPES =
      ImmutableSet.of(QuestionType.ENUMERATOR, QuestionType.STATIC);
  private final ExportServiceRepository exportServiceRepository;

  @Inject
  public CsvExporterService(
      ProgramService programService,
      QuestionService questionService,
      ApplicantService applicantService,
      Config config,
      DateConverter dateConverter,
      ExportServiceRepository exportServiceRepository) {
    this.programService = checkNotNull(programService);
    this.questionService = checkNotNull(questionService);
    this.applicantService = checkNotNull(applicantService);
    this.config = checkNotNull(config);
    this.dateConverter = dateConverter;
    this.exportServiceRepository = checkNotNull(exportServiceRepository);
  }

  /** Return a string containing a CSV of all applications at all versions of particular program. */
  public String getProgramAllVersionsCsv(
      long programId, SubmittedApplicationFilter filters, boolean isMultipleFileUploadEnabled)
      throws ProgramNotFoundException {
    ImmutableMap<Long, ProgramDefinition> programDefinitionsForAllVersions =
        programService.getAllVersionsFullProgramDefinition(programId).stream()
            .collect(ImmutableMap.toImmutableMap(ProgramDefinition::id, pd -> pd));
    ProgramDefinition currentProgram = programDefinitionsForAllVersions.get(programId);

    ImmutableList<ApplicationModel> applications =
        programService
            .getSubmittedProgramApplicationsAllVersions(
                programId,
                F.Either.Left(IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG),
                filters)
            .getPageContents();

    CsvExportConfig exportConfig =
        generateCsvConfig(
            applications,
            programDefinitionsForAllVersions,
            currentProgram.hasEligibilityEnabled(),
            isMultipleFileUploadEnabled);

    return exportCsv(
        exportConfig,
        applications,
        // Use our local program definition cache when exporting applications,
        // it's faster then the cache in the ProgramRepository.
        programDefinitionsForAllVersions::get,
        Optional.of(currentProgram));
  }

  private CsvExportConfig generateCsvConfig(
      ImmutableList<ApplicationModel> applications,
      ImmutableMap<Long, ProgramDefinition> programDefinitionsForAllVersions,
      boolean showEligibilityColumn,
      boolean isMultipleFileUploadEnabled)
      throws ProgramNotFoundException {
    Map<Path, ApplicantQuestion> uniqueQuestions = new HashMap<>();

    applications.stream()
        .flatMap(
            app ->
                applicantService
                    .getReadOnlyApplicantProgramService(
                        app, programDefinitionsForAllVersions.get(app.getProgram().id))
                    .getAllQuestions())
        .forEach(aq -> uniqueQuestions.putIfAbsent(aq.getContextualizedPath(), aq));

    ImmutableList<ApplicantQuestion> sortedUniqueQuestions =
        uniqueQuestions.values().stream()
            .sorted(Comparator.comparing(aq -> aq.getContextualizedPath().toString()))
            .collect(ImmutableList.toImmutableList());

    return buildColumnHeaders(
        sortedUniqueQuestions, showEligibilityColumn, isMultipleFileUploadEnabled);
  }

  /**
   * Export a CSV using the provided CsvExportConfig and applications.
   *
   * @param exportConfig the CsvExportConfig to use
   * @param applications the list of ApplicationModels to export
   * @param getProgramDefinition a function used to retrieve the ProgramDefinition by ID
   * @param currentProgram the current program definition
   */
  private String exportCsv(
      CsvExportConfig exportConfig,
      ImmutableList<ApplicationModel> applications,
      Function<Long, ProgramDefinition> getProgramDefinition,
      Optional<ProgramDefinition> currentProgram) {
    OutputStream inMemoryBytes = new ByteArrayOutputStream();
    try (Writer writer = new OutputStreamWriter(inMemoryBytes, StandardCharsets.UTF_8)) {
      try (CsvExporter csvExporter =
          new CsvExporter(
              exportConfig.columns(),
              config.getString("play.http.secret.key"),
              writer,
              dateConverter)) {
        boolean shouldCheckEligibility =
            currentProgram.isPresent() && currentProgram.get().hasEligibilityEnabled();

        for (ApplicationModel application : applications) {
          ProgramDefinition programDefForApplication =
              getProgramDefinition.apply(application.getProgram().id);
          ReadOnlyApplicantProgramService roApplicantService =
              applicantService.getReadOnlyApplicantProgramService(
                  application, programDefForApplication);

          Optional<Boolean> optionalEligibilityStatus =
              shouldCheckEligibility
                  ? applicantService.getApplicationEligibilityStatus(
                      application, programDefForApplication)
                  : Optional.empty();

          csvExporter.exportRecord(
              application, roApplicantService, optionalEligibilityStatus, programDefForApplication);
        }
      }
    } catch (IOException e) {
      // Since it's an in-memory writer, this shouldn't happen.  Catch so that callers don't
      // have to deal with it.
      throw new RuntimeException(e);
    }
    return inMemoryBytes.toString();
  }

  /**
   * Produce the default {@link CsvExportConfig} for a list of {@link ApplicantQuestion}s. The
   * config includes all the questions, the application id, and the application submission time.
   */
  private CsvExportConfig buildColumnHeaders(
      ImmutableList<ApplicantQuestion> exemplarQuestions,
      boolean showEligibilityColumn,
      boolean isMultipleFileUploadEnabled) {
    ImmutableList.Builder<Column> columnsBuilder = new ImmutableList.Builder<>();

    // Metadata columns
    columnsBuilder.add(
        Column.builder().setHeader("Applicant ID").setColumnType(ColumnType.APPLICANT_ID).build());
    columnsBuilder.add(
        Column.builder()
            .setHeader("Application ID")
            .setColumnType(ColumnType.APPLICATION_ID)
            .build());
    columnsBuilder.add(
        Column.builder()
            .setHeader("Applicant Language")
            .setColumnType(ColumnType.LANGUAGE)
            .build());
    columnsBuilder.add(
        Column.builder().setHeader("Submit Time").setColumnType(ColumnType.SUBMIT_TIME).build());
    columnsBuilder.add(
        Column.builder()
            .setHeader("Submitter Type")
            .setColumnType(ColumnType.SUBMITTER_TYPE)
            .build());
    columnsBuilder.add(
        Column.builder().setHeader("TI Email").setColumnType(ColumnType.TI_EMAIL).build());
    columnsBuilder.add(
        Column.builder()
            .setHeader("TI Organization")
            .setColumnType(ColumnType.TI_ORGANIZATION)
            .build());
    if (showEligibilityColumn) {
      columnsBuilder.add(
          Column.builder()
              .setHeader("Eligibility Status")
              .setColumnType(ColumnType.ELIGIBILITY_STATUS)
              .build());
    }
    columnsBuilder.add(
        Column.builder().setHeader("Status").setColumnType(ColumnType.STATUS_TEXT).build());

    // Add columns for each scalar path to an answer.
    exemplarQuestions.stream()
        .filter(aq -> !NON_EXPORTED_QUESTION_TYPES.contains(aq.getType()))
        .flatMap(
            aq ->
                aq.getType().equals(QuestionType.CHECKBOX)
                    ? buildColumnsForEveryOption(aq)
                    : buildColumnsForEveryScalar(aq, isMultipleFileUploadEnabled))
        .forEachOrdered(columnsBuilder::add);
    // Adding ADMIN_NOTE as the last coloumn to make sure it doesn't break the existing CSV exports
    columnsBuilder.add(
        Column.builder().setHeader("Admin Note").setColumnType(ColumnType.ADMIN_NOTE).build());
    return CsvExportConfig.builder().setColumns(columnsBuilder.build()).build();
  }

  private Stream<Column> buildColumnsForEveryOption(ApplicantQuestion applicantQuestion) {
    // Columns are looked up by the scalar path, so we use the scalar path here
    Path scalarPath = applicantQuestion.createMultiSelectQuestion().getSelectionPath();
    return exportServiceRepository
        .getAllHistoricMultiOptionAdminNames(applicantQuestion.getQuestionDefinition())
        .stream()
        .map(
            option ->
                Column.builder()
                    .setHeader(formatHeader(scalarPath, Optional.of(option)))
                    .setJsonPath(scalarPath)
                    .setOptionAdminName(option)
                    .setColumnType(ColumnType.APPLICANT_ANSWER)
                    .build());
  }

  private Stream<Column> buildColumnsForEveryScalar(
      ApplicantQuestion applicantQuestion, boolean isMultipleFileUploadEnabled) {
    return applicantQuestion.getQuestion().getAllPaths().stream()
        .filter(
            p ->
                !(isMultipleFileUploadEnabled
                    && p.keyName()
                        .equals(Scalar.FILE_KEY.toString().toLowerCase(Locale.getDefault()))))
        .map(
            path ->
                Column.builder()
                    .setHeader(formatHeader(path, Optional.empty()))
                    .setJsonPath(path)
                    .setColumnType(ColumnType.APPLICANT_ANSWER)
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
   * @param path is a path that ends in a {@link services.applicant.question.Scalar}
   * @param optionAdminName the admin name of the multi-option question option, if it's a
   *     multi-option question
   */
  @VisibleForTesting
  static String formatHeader(Path path, String optionAdminName) {
    return formatHeader(path, Optional.of(optionAdminName));
  }

  @VisibleForTesting
  static String formatHeader(Path path) {
    return formatHeader(path, Optional.empty());
  }

  private static String formatHeader(Path path, Optional<String> optionAdminName) {
    Path finalPath = path;
    String scalarComponent =
        optionAdminName
            .map(o -> String.format("(%s - %s)", finalPath.keyName(), o))
            .orElse(String.format("(%s)", finalPath.keyName()));
    // Remove "cents" from the currency string as the value will be dollars.
    if (path.keyName().equals(CURRENCY_CENTS_TYPE_STRING)) {
      scalarComponent = "(currency)";
    }

    // Remove "name" from the name suffix string as it will be indicated in the name.
    if (path.keyName().equals(NAME_SUFFIX)) {
      scalarComponent = "(suffix)";
    }

    // Change scalar name for file_key_list
    if (path.keyName().equals(FILE_KEY_LIST)) {
      scalarComponent = "(file_urls)";
    }

    // TODO: #7134 Only here for backwards compatibility. Long term this should go away
    if (path.keyName().equals(SERVICE_AREAS)) {
      scalarComponent = String.format("(%s)", SERVICE_AREA);
    }

    List<String> reversedHeaderComponents = new ArrayList<>(Arrays.asList(scalarComponent));
    while (!path.parentPath().isEmpty()
        && !path.parentPath().equals(ApplicantData.APPLICANT_PATH)) {
      path = path.parentPath();
      String headerComponent = path.keyName().replace("_", " ");
      reversedHeaderComponents.add(headerComponent);
    }

    // The pieces to the header are build in reverse, as we reference path#parentPath(), so we build
    // the header string
    // going backwards through the list.
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

  /**
   * A string containing the CSV which maps applicants (opaquely) to the programs they applied to.
   * TODO(#6746): Include repeated questions in the demographic export
   */
  public String getDemographicsCsv(TimeFilter filter) {
    // Use the ProgramDefinition cache in the ProgramRepository, since we don't already have a local
    // cache of ProgramDefinitions. This will cause a database call for program definitions that
    // aren't yet in the cache.
    // TODO(#8147): Consider warming the program definition cache with definitions for all
    // programs.
    Function<Long, ProgramDefinition> getProgramDefinition =
        (id) -> {
          try {
            return programService.getFullProgramDefinition(id);
          } catch (ProgramNotFoundException e) {
            // This shouldn't happen, we used a known program ID when requesting the program
            // definition
            throw new RuntimeException(e);
          }
        };
    return exportCsv(
        getDemographicsExporterConfig(),
        applicantService.getApplications(filter),
        getProgramDefinition,
        /* currentProgram= */ Optional.empty());
  }

  private CsvExportConfig getDemographicsExporterConfig() {
    ImmutableList.Builder<Column> columnsBuilder = new ImmutableList.Builder<>();

    // First add the ID, submit time, and submitter email columns.
    columnsBuilder.add(
        Column.builder().setHeader("Opaque ID").setColumnType(ColumnType.OPAQUE_ID).build());
    columnsBuilder.add(
        Column.builder().setHeader("Program").setColumnType(ColumnType.PROGRAM).build());
    columnsBuilder.add(
        Column.builder()
            .setHeader("Submitter Type")
            .setColumnType(ColumnType.SUBMITTER_TYPE)
            .build());
    columnsBuilder.add(
        Column.builder()
            .setHeader("TI Email (Opaque)")
            .setColumnType(ColumnType.TI_EMAIL_OPAQUE)
            .build());
    columnsBuilder.add(
        Column.builder()
            .setHeader("TI Organization")
            .setColumnType(ColumnType.TI_ORGANIZATION)
            .build());
    columnsBuilder.add(
        Column.builder().setHeader("Create Time").setColumnType(ColumnType.CREATE_TIME).build());
    columnsBuilder.add(
        Column.builder().setHeader("Submit Time").setColumnType(ColumnType.SUBMIT_TIME).build());
    columnsBuilder.add(
        Column.builder().setHeader("Status").setColumnType(ColumnType.STATUS_TEXT).build());

    // We need this outer for-loop because the export type question tag isn't available on the
    // question definition.
    for (QuestionTag tagType :
        ImmutableList.of(QuestionTag.DEMOGRAPHIC, QuestionTag.DEMOGRAPHIC_PII)) {
      this.questionService.getQuestionsForTag(tagType).stream()
          // Do not include Enumerator answers in CSVs.
          .filter(qd -> !NON_EXPORTED_QUESTION_TYPES.contains(qd.getQuestionType()))
          // Use a program question definition that doesn't have a program associated with it,
          // which is okay because this should be program agnostic.
          .map(qd -> ProgramQuestionDefinition.create(qd, Optional.empty()))
          .map(pqd -> new ApplicantQuestion(pqd, new ApplicantData(), Optional.empty()))
          .flatMap(
              aq -> {
                if (aq.getType().equals(QuestionType.CHECKBOX)) {
                  // Columns are looked up by the scalar path, so we use the scalar path here
                  Path path = aq.createMultiSelectQuestion().getSelectionPath();
                  return exportServiceRepository
                      .getAllHistoricMultiOptionAdminNames(aq.getQuestionDefinition())
                      .stream()
                      .map(
                          option ->
                              Column.builder()
                                  .setHeader(formatHeader(path, option))
                                  .setJsonPath(path)
                                  .setOptionAdminName(option)
                                  .setColumnType(
                                      tagType == QuestionTag.DEMOGRAPHIC_PII
                                          ? ColumnType.APPLICANT_OPAQUE
                                          : ColumnType.APPLICANT_ANSWER)
                                  .build());
                }

                return aq.getQuestion().getAllPaths().stream()
                    .map(
                        path ->
                            Column.builder()
                                .setHeader(formatHeader(path))
                                .setJsonPath(path)
                                .setColumnType(
                                    tagType == QuestionTag.DEMOGRAPHIC_PII
                                        ? ColumnType.APPLICANT_OPAQUE
                                        : ColumnType.APPLICANT_ANSWER)
                                .build());
              })
          .forEachOrdered(columnsBuilder::add);
    }

    return CsvExportConfig.builder().setColumns(columnsBuilder.build()).build();
  }
}
