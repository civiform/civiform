package services.export;

import static com.google.common.base.Preconditions.checkNotNull;

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
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import javax.inject.Inject;
import models.ApplicantModel;
import models.ApplicationModel;
import models.QuestionTag;
import repository.SubmittedApplicationFilter;
import repository.TimeFilter;
import services.DateConverter;
import services.Path;
import services.applicant.ApplicantData;
import services.applicant.ApplicantService;
import services.applicant.ReadOnlyApplicantProgramService;
import services.applicant.question.ApplicantQuestion;
import services.export.enums.ColumnType;
import services.pagination.SubmitTimeSequentialAccessPaginationSpec;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramQuestionDefinition;
import services.program.ProgramService;
import services.question.QuestionService;
import services.question.types.QuestionType;

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
  private final CsvColumnFactory csvColumnFactory;

  public static final ImmutableSet<QuestionType> NON_EXPORTED_QUESTION_TYPES =
      ImmutableSet.of(QuestionType.ENUMERATOR, QuestionType.STATIC);

  @Inject
  public CsvExporterService(
      ProgramService programService,
      QuestionService questionService,
      ApplicantService applicantService,
      Config config,
      DateConverter dateConverter,
      CsvColumnFactory csvColumnFactory) {
    this.programService = checkNotNull(programService);
    this.questionService = checkNotNull(questionService);
    this.applicantService = checkNotNull(applicantService);
    this.config = checkNotNull(config);
    this.dateConverter = dateConverter;
    this.csvColumnFactory = checkNotNull(csvColumnFactory);
  }

  /** Return a string containing a CSV of all applications at all versions of particular program. */
  public String getProgramAllVersionsCsv(long programId, SubmittedApplicationFilter filters)
      throws ProgramNotFoundException {
    ImmutableMap<Long, ProgramDefinition> programDefinitionsForAllVersions =
        programService.getAllVersionsFullProgramDefinition(programId).stream()
            .collect(ImmutableMap.toImmutableMap(ProgramDefinition::id, pd -> pd));
    ProgramDefinition currentProgram = programDefinitionsForAllVersions.get(programId);

    ImmutableList<ApplicationModel> applications =
        programService
            .getSubmittedProgramApplicationsAllVersions(
                programId,
                SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
                filters)
            .getPageContents();

    CsvExportConfig exportConfig =
        generateCsvConfig(
            applications, programDefinitionsForAllVersions, currentProgram.hasEligibilityEnabled());

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
      boolean showEligibilityColumn)
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
            // TODO(#9196): This sorts the paths lexicographically, so
            // "household members[10] - name" is sorted above "household members[1] - name".
            // It should be possible to write a comparator that iteratively compares segments of
            // the Path, so that nested repeated questions are sorted correctly.
            .sorted(Comparator.comparing(aq -> aq.getContextualizedPath().toString()))
            .collect(ImmutableList.toImmutableList());

    return buildColumnHeaders(sortedUniqueQuestions, showEligibilityColumn);
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
      ImmutableList<ApplicantQuestion> exemplarQuestions, boolean showEligibilityColumn) {
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
        .flatMap(aq -> csvColumnFactory.buildColumns(aq, ColumnType.APPLICANT_ANSWER))
        .forEachOrdered(columnsBuilder::add);
    // Adding ADMIN_NOTE as the last coloumn to make sure it doesn't break the existing CSV exports
    columnsBuilder.add(
        Column.builder().setHeader("Admin Note").setColumnType(ColumnType.ADMIN_NOTE).build());
    columnsBuilder.add(
        Column.builder()
            .setHeader("Status Last Modified Time")
            .setColumnType(ColumnType.STATUS_LAST_MODIFIED_TIME)
            .build());
    return CsvExportConfig.builder().setColumns(columnsBuilder.build()).build();
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
          .map(
              pqd ->
                  new ApplicantQuestion(
                      pqd, new ApplicantModel(), new ApplicantData(), Optional.empty()))
          .flatMap(
              aq ->
                  csvColumnFactory.buildColumns(
                      aq,
                      tagType == QuestionTag.DEMOGRAPHIC_PII
                          ? ColumnType.APPLICANT_OPAQUE
                          : ColumnType.APPLICANT_ANSWER))
          .forEachOrdered(columnsBuilder::add);
    }

    return CsvExportConfig.builder().setColumns(columnsBuilder.build()).build();
  }
}
