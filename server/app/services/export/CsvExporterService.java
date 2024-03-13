package services.export;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
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
import java.util.stream.Stream;
import javax.inject.Inject;
import models.ApplicationModel;
import models.QuestionTag;
import play.libs.F;
import play.mvc.Http.Request;
import repository.ExportServiceRepository;
import repository.SubmittedApplicationFilter;
import repository.TimeFilter;
import services.DateConverter;
import services.IdentifierBasedPaginationSpec;
import services.Path;
import services.applicant.AnswerData;
import services.applicant.ApplicantData;
import services.applicant.ApplicantService;
import services.applicant.ReadOnlyApplicantProgramService;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.MultiSelectQuestion;
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
      long programId, SubmittedApplicationFilter filters, Request request)
      throws ProgramNotFoundException {
    ImmutableList<ProgramDefinition> allProgramVersions =
        programService.getAllVersionsFullProgramDefinition(programId).stream()
            .collect(ImmutableList.toImmutableList());

    ProgramDefinition currentProgram = programService.getFullProgramDefinition(programId);
    CsvExportConfig exportConfig =
        generateAllProgramVersionsCsvExportConfig(
            allProgramVersions, currentProgram.hasEligibilityEnabled());

    ImmutableList<ApplicationModel> applications =
        programService
            .getSubmittedProgramApplicationsAllVersions(
                programId,
                F.Either.Left(IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG),
                filters,
                request)
            .getPageContents();

    return exportCsv(exportConfig, applications, Optional.of(currentProgram));
  }

  private CsvExportConfig generateAllProgramVersionsCsvExportConfig(
      ImmutableList<ProgramDefinition> programDefinitions, boolean showEligibilityColumn)
      throws ProgramNotFoundException {
    Map<Path, AnswerData> answerMap = new HashMap<>();

    for (ProgramDefinition programDefinition : programDefinitions) {
      for (ApplicationModel application :
          programService.getSubmittedProgramApplications(programDefinition.id())) {
        applicantService
            .getReadOnlyApplicantProgramService(application, programDefinition)
            .getSummaryDataOnlyActive()
            .forEach(data -> answerMap.putIfAbsent(data.contextualizedPath(), data));
      }
    }

    // Get the list of all answers, sorted by block ID, then question index, and finally
    // contextualized path in string form.
    ImmutableList<AnswerData> answers =
        answerMap.values().stream()
            .sorted(
                Comparator.comparing(AnswerData::blockId)
                    .thenComparing(AnswerData::questionIndex)
                    .thenComparing(answerData -> answerData.contextualizedPath().toString()))
            .collect(ImmutableList.toImmutableList());

    return buildColumnHeaders(answers, showEligibilityColumn);
  }

  /**
   * Return a string containing a CSV of all applications for a specific program version.
   *
   * @throws ProgramNotFoundException If the program ID refers to a program that does not exist.
   */
  public String getProgramCsv(long programId) throws ProgramNotFoundException {
    ImmutableList<ApplicationModel> applications =
        programService.getSubmittedProgramApplications(programId);
    ProgramDefinition programDefinition = programService.getFullProgramDefinition(programId);
    return exportCsv(
        generateSingleProgramVersionCsvExportConfig(
            programId, programDefinition.hasEligibilityEnabled()),
        applications,
        Optional.of(programDefinition));
  }

  private String exportCsv(
      CsvExportConfig exportConfig,
      ImmutableList<ApplicationModel> applications,
      Optional<ProgramDefinition> currentProgram) {
    OutputStream inMemoryBytes = new ByteArrayOutputStream();
    try (Writer writer = new OutputStreamWriter(inMemoryBytes, StandardCharsets.UTF_8)) {
      try (CsvExporter csvExporter =
          new CsvExporter(
              exportConfig.columns(),
              config.getString("play.http.secret.key"),
              writer,
              dateConverter)) {
        // Cache Program data which doesn't change, so we only look it up once rather than on every
        // exported row.
        // TODO(#1750): Lookup all relevant programs in one request to reduce cost of N lookups.
        // TODO(#1750): Consider Play's JavaCache over this caching.
        HashMap<Long, ProgramDefinition> programDefinitions = new HashMap<>();
        boolean shouldCheckEligibility =
            currentProgram.isPresent() && currentProgram.get().hasEligibilityEnabled();
        for (ApplicationModel application : applications) {
          Long programId = application.getProgram().id;
          if (!programDefinitions.containsKey(programId)) {
            try {
              programDefinitions.put(programId, programService.getFullProgramDefinition(programId));
            } catch (ProgramNotFoundException e) {
              throw new RuntimeException("Cannot find a program that has applications for it.", e);
            }
          }
          ProgramDefinition programDefinition = programDefinitions.get(programId);

          ReadOnlyApplicantProgramService roApplicantService =
              applicantService.getReadOnlyApplicantProgramService(application, programDefinition);

          Optional<Boolean> optionalEligibilityStatus =
              shouldCheckEligibility
                  ? applicantService.getApplicationEligibilityStatus(application, programDefinition)
                  : Optional.empty();

          csvExporter.exportRecord(application, roApplicantService, optionalEligibilityStatus);
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
   * Produce the CSV config for a given program. The config includes the application id, the
   * application submission time, and all possible scalar values from all of its applications. This
   * means if one application had a question repeated for N repeated entities, then there would be N
   * columns for each of that question's scalars.
   */
  private CsvExportConfig generateSingleProgramVersionCsvExportConfig(
      long programId, boolean showEligibilityColumn) {
    ImmutableList<ApplicationModel> applications;

    try {
      applications = programService.getSubmittedProgramApplications(programId);
    } catch (ProgramNotFoundException e) {
      throw new RuntimeException("Cannot find a program we are trying to generate CSVs for.", e);
    }

    // Create a map, keyed by <block id, question index>, holding an AnswerData for every <block id,
    // question index> in all the applications
    // for this program. The content of the answer doesn't matter, only that we have one
    // representing every  <block id, question index>.
    Map<String, AnswerData> representativeAnswerMap = new HashMap<>();
    for (ApplicationModel application : applications) {
      ReadOnlyApplicantProgramService roApplicantService =
          applicantService
              .getReadOnlyApplicantProgramService(application)
              .toCompletableFuture()
              .join();
      roApplicantService
          .getSummaryDataOnlyActive()
          .forEach(
              data -> representativeAnswerMap.putIfAbsent(representativeAnswersKey(data), data));
    }

    // Get the list of all representative answers, sorted by block ID and question index, and
    // generate the
    // csv config.
    ImmutableList<AnswerData> representativeAnswers =
        representativeAnswerMap.values().stream()
            .sorted(
                Comparator.comparing(AnswerData::blockId).thenComparing(AnswerData::questionIndex))
            .collect(ImmutableList.toImmutableList());
    return buildColumnHeaders(representativeAnswers, showEligibilityColumn);
  }

  /**
   * Produce the default {@link CsvExportConfig} for a list of {@link AnswerData}s. The config
   * includes all the questions, the application id, and the application submission time.
   */
  private CsvExportConfig buildColumnHeaders(
      ImmutableList<AnswerData> representativeAnswers, boolean showEligibilityColumn) {
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

    // Add columns for each path to an answer.
    representativeAnswers.stream()
        .filter(
            answerData ->
                !NON_EXPORTED_QUESTION_TYPES.contains(
                    answerData.questionDefinition().getQuestionType()))
        .flatMap(
            answerData ->
                answerData.questionDefinition().getQuestionType().equals(QuestionType.CHECKBOX)
                    ? buildColumnsForEveryOption(answerData)
                    : buildColumnsForEveryScalar(answerData))
        .forEachOrdered(columnsBuilder::add);

    return CsvExportConfig.builder().setColumns(columnsBuilder.build()).build();
  }

  private Stream<Column> buildColumnsForEveryOption(AnswerData answerData) {
    // Columns are looked up by the scalar path, so we use the scalar path here
    Path path = ((MultiSelectQuestion) answerData.createQuestion()).getSelectionPath();
    return exportServiceRepository
        .getAllHistoricMultiOptionAdminNames(answerData.questionDefinition())
        .stream()
        .map(
            option ->
                Column.builder()
                    .setHeader(formatHeader(path, Optional.of(option)))
                    .setJsonPath(path)
                    .setOptionAdminName(option)
                    .setColumnType(ColumnType.APPLICANT_ANSWER)
                    .build());
  }

  private Stream<Column> buildColumnsForEveryScalar(AnswerData answerData) {
    return answerData.scalarAnswersInDefaultLocale().keySet().stream()
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
   * A useful string that uniquely identifies an answer within an applicant program and is shared
   * across applicant programs.
   */
  private static String representativeAnswersKey(AnswerData answerData) {
    return String.format("%s-%d", answerData.blockId(), answerData.questionIndex());
  }

  /**
   * A string containing the CSV which maps applicants (opaquely) to the programs they applied to.
   * TODO(#6746): Include repeated questions in the demographic export
   */
  public String getDemographicsCsv(TimeFilter filter) {
    return exportCsv(
        getDemographicsExporterConfig(),
        applicantService.getApplications(filter),
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
