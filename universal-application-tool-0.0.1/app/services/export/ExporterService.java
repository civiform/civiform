package services.export;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
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
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import models.Application;
import models.QuestionTag;
import services.Path;
import services.applicant.AnswerData;
import services.applicant.ApplicantData;
import services.applicant.ApplicantService;
import services.applicant.ReadOnlyApplicantProgramService;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.PresentsErrors;
import services.program.Column;
import services.program.ColumnType;
import services.program.CsvExportConfig;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramQuestionDefinition;
import services.program.ProgramService;
import services.question.QuestionService;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;

/**
 * ExporterService generates CSV files for applications to a program or demographic information
 * across all programs.
 */
public class ExporterService {
  private final ExporterFactory exporterFactory;
  private final ProgramService programService;
  private final QuestionService questionService;
  private final ApplicantService applicantService;

  private static final String HEADER_SPACER_ENUM = " - ";
  private static final String HEADER_SPACER_SCALAR = " ";

  public static final ImmutableSet<QuestionType> NON_EXPORTED_QUESTION_TYPES =
      ImmutableSet.of(QuestionType.ENUMERATOR, QuestionType.STATIC);

  @Inject
  public ExporterService(
      ExporterFactory exporterFactory,
      ProgramService programService,
      QuestionService questionService,
      ApplicantService applicantService) {
    this.exporterFactory = checkNotNull(exporterFactory);
    this.programService = checkNotNull(programService);
    this.questionService = checkNotNull(questionService);
    this.applicantService = checkNotNull(applicantService);
  }

  /**
   * Return a string containing the CSV of all the applicantions for a particular program.
   *
   * @throws ProgramNotFoundException If the program ID refers to a program that does not exist.
   */
  public String getProgramCsv(long programId) throws ProgramNotFoundException {
    ImmutableList<Application> applications = programService.getProgramApplications(programId);
    ProgramDefinition program = programService.getProgramDefinition(programId);
    CsvExporter csvExporter;
    if (program.exportDefinitions().stream()
        .anyMatch(exportDefinition -> exportDefinition.csvConfig().isPresent())) {
      csvExporter = exporterFactory.csvExporter(program.toProgram());
    } else {
      csvExporter = exporterFactory.csvExporter(generateDefaultCsvConfig(programId));
    }
    return exportCsv(csvExporter, applications);
  }

  public String exportCsv(CsvExporter csvExporter, ImmutableList<Application> applications) {
    try {
      OutputStream inMemoryBytes = new ByteArrayOutputStream();
      Writer writer = new OutputStreamWriter(inMemoryBytes, StandardCharsets.UTF_8);
      for (Application application : applications) {
        ReadOnlyApplicantProgramService roApplicantService =
            applicantService
                .getReadOnlyApplicantProgramService(application)
                .toCompletableFuture()
                .join();
        csvExporter.export(application, roApplicantService, writer);
      }
      writer.close();
      return inMemoryBytes.toString();
    } catch (IOException e) {
      // Since it's an in-memory writer, this shouldn't happen.  Catch so that callers don't
      // have to deal with it.
      throw new RuntimeException(e);
    }
  }

  /**
   * Produce the default CSV config for a given program. The default config includes the application
   * id, the application submission time, and all possible scalar values from all of its
   * applications. This means if one application had a question repeated for N repeated entities,
   * then there would be N columns for each of that question's scalars.
   */
  CsvExportConfig generateDefaultCsvConfig(long programId) {
    ImmutableList<Application> applications;
    try {
      applications = programService.getProgramApplications(programId);
    } catch (ProgramNotFoundException e) {
      throw new RuntimeException("Cannot find a program we are trying to generate CSVs for.", e);
    }

    // Create a map from a key <block id, question index> to an answer with every application. It
    // doesn't matter which answer ends up in the map, as long as every <block id, question index>
    // is accounted for.
    Map<String, AnswerData> answerMap = new HashMap<>();
    for (Application application : applications) {
      ReadOnlyApplicantProgramService roApplicantService =
          applicantService
              .getReadOnlyApplicantProgramService(application)
              .toCompletableFuture()
              .join();
      roApplicantService
          .getSummaryData()
          .forEach(data -> answerMap.putIfAbsent(answerDataKey(data), data));
    }

    // Get the list of all answers, sorted by block ID and question index, and generate the default
    // csv config.
    ImmutableList<AnswerData> answers =
        answerMap.values().stream()
            .sorted(
                Comparator.comparing(AnswerData::blockId).thenComparing(AnswerData::questionIndex))
            .collect(ImmutableList.toImmutableList());
    return generateDefaultCsvConfig(answers);
  }

  /**
   * Produce the default {@link CsvExportConfig} for a list of {@link AnswerData}s. The default
   * config includes all the questions, the application id, and the application submission time.
   */
  private CsvExportConfig generateDefaultCsvConfig(ImmutableList<AnswerData> answerDataList) {
    ImmutableList.Builder<Column> columnsBuilder = new ImmutableList.Builder<>();
    // First add the ID, submit time, and submitter email columns.
    columnsBuilder.add(Column.builder().setHeader("ID").setColumnType(ColumnType.ID).build());
    columnsBuilder.add(
        Column.builder()
            .setHeader("Applicant language")
            .setColumnType(ColumnType.LANGUAGE)
            .build());
    columnsBuilder.add(
        Column.builder().setHeader("Submit time").setColumnType(ColumnType.SUBMIT_TIME).build());
    columnsBuilder.add(
        Column.builder()
            .setHeader("Submitted by")
            .setColumnType(ColumnType.SUBMITTER_EMAIL)
            .build());

    // Add columns for each path to an answer.
    for (AnswerData answerData : answerDataList) {
      if (answerData.questionDefinition().isEnumerator()) {
        continue; // Do not include Enumerator answers in CSVs.
      }
      for (Path path : answerData.scalarAnswersInDefaultLocale().keySet()) {
        columnsBuilder.add(
            Column.builder()
                .setHeader(pathToHeader(path))
                .setJsonPath(path)
                .setColumnType(ColumnType.APPLICANT)
                .build());
      }
    }
    return new CsvExportConfig() {
      @Override
      public ImmutableList<Column> columns() {
        return columnsBuilder.build();
      }
    };
  }

  /**
   * Convert {@link Path} to a human readable header string.
   *
   * <p>The {@link ApplicantData#APPLICANT_PATH} is ignored, enumerator references are separated by
   * {@link #HEADER_SPACER_ENUM} and the scalar is separated by {@link #HEADER_SPACER_SCALAR}.
   *
   * <p>Example: "applicant.household_members[3].household_member_name.first_name" becomes
   * "household members[3] - household member name (first_name)"
   *
   * @param path is a path that ends in a {@link services.applicant.question.Scalar}.
   */
  @VisibleForTesting
  static String pathToHeader(Path path) {
    String scalarComponent = String.format("(%s)", path.keyName());
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
  private static String answerDataKey(AnswerData answerData) {
    return String.format("%s-%d", answerData.blockId(), answerData.questionIndex());
  }

  /**
   * A string containing the CSV which maps applicants (opaquely) to the programs they applied to.
   */
  public String getDemographicsCsv() {
    return exportCsv(
        exporterFactory.csvExporter(getDemographicsExporterConfig()),
        applicantService.getAllApplications());
  }

  public CsvExportConfig getDemographicsExporterConfig() {
    ImmutableList.Builder<Column> columnsBuilder = new ImmutableList.Builder<>();
    // First add the ID, submit time, and submitter email columns.
    columnsBuilder.add(
        Column.builder().setHeader("Opaque ID").setColumnType(ColumnType.OPAQUE_ID).build());
    columnsBuilder.add(
        Column.builder().setHeader("Program").setColumnType(ColumnType.PROGRAM).build());
    columnsBuilder.add(
        Column.builder()
            .setHeader("Submitter Email (Opaque)")
            .setColumnType(ColumnType.SUBMITTER_EMAIL_OPAQUE)
            .build());
    columnsBuilder.add(
        Column.builder()
            .setHeader("TI Organization")
            .setColumnType(ColumnType.TI_ORGANIZATION)
            .build());
    columnsBuilder.add(
        Column.builder().setHeader("Create time").setColumnType(ColumnType.CREATE_TIME).build());
    columnsBuilder.add(
        Column.builder().setHeader("Submit time").setColumnType(ColumnType.SUBMIT_TIME).build());

    for (QuestionTag tagType :
        ImmutableList.of(QuestionTag.DEMOGRAPHIC, QuestionTag.DEMOGRAPHIC_PII)) {
      for (QuestionDefinition questionDefinition :
          this.questionService.getQuestionsForTag(tagType)) {
        if (NON_EXPORTED_QUESTION_TYPES.contains(questionDefinition.getQuestionType())) {
          continue; // Do not include Enumerator answers in CSVs.
        }
        // Use a program question definition that doesn't have a program associated with it,
        // which is okay because this should be program agnostic.
        ProgramQuestionDefinition pqd =
            ProgramQuestionDefinition.create(questionDefinition, Optional.empty());
        PresentsErrors applicantQuestion =
            new ApplicantQuestion(pqd, new ApplicantData(), Optional.empty()).errorsPresenter();
        for (Path path : applicantQuestion.getAllPaths()) {
          columnsBuilder.add(
              Column.builder()
                  .setHeader(pathToHeader(path))
                  .setJsonPath(path)
                  .setColumnType(
                      tagType == QuestionTag.DEMOGRAPHIC_PII
                          ? ColumnType.APPLICANT_OPAQUE
                          : ColumnType.APPLICANT)
                  .build());
        }
      }
    }
    return new CsvExportConfig() {
      @Override
      public ImmutableList<Column> columns() {
        return columnsBuilder.build();
      }
    };
  }
}
