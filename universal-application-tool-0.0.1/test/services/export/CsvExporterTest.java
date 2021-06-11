package services.export;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Streams;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import models.Applicant;
import models.Application;
import models.LifecycleStage;
import models.Program;
import models.Question;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.Before;
import org.junit.Test;
import repository.WithPostgresContainer;
import services.Path;
import services.applicant.ApplicantData;
import services.applicant.ApplicantService;
import services.applicant.ReadOnlyApplicantProgramService;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.FileUploadQuestion;
import services.applicant.question.MultiSelectQuestion;
import services.applicant.question.NameQuestion;
import services.applicant.question.Scalar;
import services.program.Column;
import services.program.ColumnType;
import services.program.CsvExportConfig;
import services.program.ExportDefinition;
import services.program.ExportEngine;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;
import support.ProgramBuilder;
import support.QuestionAnswerer;

public class CsvExporterTest extends WithPostgresContainer {
  private ImmutableList<Question> fakeQuestions;
  private Program fakeProgramWithCsvExport;
  private ImmutableList<Applicant> fakeApplicants;
  private Writer writer;
  private ByteArrayOutputStream inMemoryBytes;

  private void createFakeQuestions() {
    this.fakeQuestions =
        testQuestionBank.getSampleQuestionsForAllTypes().values().stream()
            .sorted(Comparator.comparing(question -> question.getQuestionDefinition().getName()))
            .collect(ImmutableList.toImmutableList());
  }

  private ApplicantQuestion getApplicantQuestion(QuestionDefinition questionDefinition) {
    return new ApplicantQuestion(questionDefinition, new ApplicantData(), Optional.empty());
  }

  private void createFakeProgram() {
    createFakeQuestions();
    ProgramBuilder fakeProgram = ProgramBuilder.newDraftProgram();
    CsvExportConfig.Builder csvExportConfigBuilder = CsvExportConfig.builder();
    fakeQuestions.forEach(
        question -> {
          Program program = fakeProgram.withBlock().withQuestion(question).build();
          long blockId = program.getProgramDefinition().getMaxBlockDefinitionId();
          QuestionDefinition questionDefinition = question.getQuestionDefinition();
          if (questionDefinition.isEnumerator()) {
            return;
          }
          getApplicantQuestion(questionDefinition).getContextualizedScalars().keySet().stream()
              .filter(path -> !Scalar.getMetadataScalarKeys().contains(path.keyName()))
              .sorted(Comparator.comparing(Path::keyName))
              .forEach(
                  path ->
                      csvExportConfigBuilder.addColumn(
                          Column.builder()
                              .setHeader(ExporterService.pathToHeader(path))
                              .setAnswerDataKey(String.format("%d-0", blockId))
                              .setJsonPath(path)
                              .setColumnType(ColumnType.APPLICANT)
                              .build()));
        });
    this.fakeProgramWithCsvExport =
        fakeProgram
            .withExportDefinition(
                ExportDefinition.builder()
                    .setEngine(ExportEngine.CSV)
                    .setCsvConfig(Optional.of(csvExportConfigBuilder.build()))
                    .build())
            .build();
  }

  private void answerQuestion(
      QuestionType questionType,
      Question question,
      ApplicantData applicantDataOne,
      ApplicantData applicantDataTwo) {
    Path answerPath =
        question
            .getQuestionDefinition()
            .getContextualizedPath(Optional.empty(), ApplicantData.APPLICANT_PATH);
    switch (questionType) {
      case ADDRESS:
        QuestionAnswerer.answerAddressQuestion(
            applicantDataOne, answerPath, "street st", "apt 100", "city", "AB", "54321");
        // applicant two did not answer this question.
        break;
      case CHECKBOX:
        QuestionAnswerer.answerMultiSelectQuestion(applicantDataOne, answerPath, 0, 1L);
        QuestionAnswerer.answerMultiSelectQuestion(applicantDataOne, answerPath, 1, 2L);
        // applicant two did not answer this question.
        break;
      case DATE:
        QuestionAnswerer.answerDateQuestion(applicantDataOne, answerPath, "1980-01-01");
        // applicant two did not answer this question.
        break;
      case DROPDOWN:
        QuestionAnswerer.answerSingleSelectQuestion(applicantDataOne, answerPath, 2L);
        // applicant two did not answer this question.
        break;
      case EMAIL:
        QuestionAnswerer.answerEmailQuestion(applicantDataOne, answerPath, "one@example.com");
        // applicant two did not answer this question.
        break;
      case FILEUPLOAD:
        QuestionAnswerer.answerFileQuestion(applicantDataOne, answerPath, "my-file-key");
        // applicant two did not answer this question.
        break;
      case NAME:
        QuestionAnswerer.answerNameQuestion(applicantDataOne, answerPath, "Alice", "", "Appleton");
        QuestionAnswerer.answerNameQuestion(applicantDataTwo, answerPath, "Bob", "", "Baker");
        break;
      case NUMBER:
        QuestionAnswerer.answerNumberQuestion(applicantDataOne, answerPath, "123456");
        // applicant two did not answer this question.
        break;
      case RADIO_BUTTON:
        QuestionAnswerer.answerSingleSelectQuestion(applicantDataOne, answerPath, 1L);
        // applicant two did not answer this question.
        break;
      case ENUMERATOR:
        QuestionAnswerer.answerEnumeratorQuestion(
            applicantDataOne, answerPath, ImmutableList.of("item1", "item2"));
        // applicant two did not answer this question.
        break;
      case TEXT:
        QuestionAnswerer.answerTextQuestion(
            applicantDataOne, answerPath, "Some Value \" containing ,,, special characters");
        // applicant two did not answer this question.
        break;
    }
  }

  private void createFakeApplicants() {
    Applicant fakeApplicantOne = new Applicant();
    Applicant fakeApplicantTwo = new Applicant();
    testQuestionBank.getSampleQuestionsForAllTypes().entrySet().stream()
        .forEach(
            entry ->
                answerQuestion(
                    entry.getKey(),
                    entry.getValue(),
                    fakeApplicantOne.getApplicantData(),
                    fakeApplicantTwo.getApplicantData()));
    fakeApplicantOne.save();
    fakeApplicantTwo.save();
    this.fakeApplicants = ImmutableList.of(fakeApplicantOne, fakeApplicantTwo);
  }

  public void createInMemoryWriter() {
    this.inMemoryBytes = new ByteArrayOutputStream();
    this.writer = new OutputStreamWriter(inMemoryBytes, StandardCharsets.UTF_8);
  }

  @Before
  public void setUp() {
    createFakeProgram();
    createFakeApplicants();
    createInMemoryWriter();
  }

  @Test
  public void fillOutCsv() throws Exception {
    ApplicantService applicantService = instanceOf(ApplicantService.class);
    ExporterFactory exporterFactory = instanceOf(ExporterFactory.class);
    CsvExporter exporter = exporterFactory.csvExporter(fakeProgramWithCsvExport);
    for (Applicant applicant : fakeApplicants) {
      Application application =
          new Application(applicant, fakeProgramWithCsvExport, LifecycleStage.ACTIVE);
      application.save();
      ReadOnlyApplicantProgramService roApplicantService =
          applicantService
              .getReadOnlyApplicantProgramService(application)
              .toCompletableFuture()
              .get();
      exporter.export(application, roApplicantService, writer);
    }
    writer.close();

    CSVParser parser =
        CSVParser.parse(
            inMemoryBytes.toString(StandardCharsets.UTF_8),
            CSVFormat.DEFAULT.withFirstRecordAsHeader());
    List<CSVRecord> records = parser.getRecords();
    assertThat(records).hasSize(2);
    Streams.mapWithIndex(
            fakeQuestions.stream()
                .filter(question -> !question.getQuestionDefinition().isEnumerator())
                .flatMap(
                    question ->
                        getApplicantQuestion(question.getQuestionDefinition())
                            .getContextualizedScalars()
                            .keySet()
                            .stream()
                            .filter(
                                path -> !Scalar.getMetadataScalarKeys().contains(path.keyName()))
                            .sorted(Comparator.comparing(Path::keyName))),
            (path, index) -> new AbstractMap.SimpleEntry<Path, Integer>(path, (int) index))
        .forEach(
            entry ->
                assertThat(parser.getHeaderMap())
                    .containsEntry(ExporterService.pathToHeader(entry.getKey()), entry.getValue()));

    Question nameQuestion = testQuestionBank.getSampleQuestionsForAllTypes().get(QuestionType.NAME);
    NameQuestion nameApplicantQuestion =
        getApplicantQuestion(nameQuestion.getQuestionDefinition()).createNameQuestion();
    String firstNameHeader = ExporterService.pathToHeader(nameApplicantQuestion.getFirstNamePath());
    String lastNameHeader = ExporterService.pathToHeader(nameApplicantQuestion.getLastNamePath());
    assertThat(records.get(0).get(firstNameHeader)).isEqualTo("Alice");
    assertThat(records.get(1).get(lastNameHeader)).isEqualTo("Baker");
    // Check list for multiselect in default locale
    Question checkboxQuestion =
        testQuestionBank.getSampleQuestionsForAllTypes().get(QuestionType.CHECKBOX);
    MultiSelectQuestion multiSelectApplicantQuestion =
        getApplicantQuestion(checkboxQuestion.getQuestionDefinition()).createMultiSelectQuestion();
    String multiSelectHeader =
        ExporterService.pathToHeader(multiSelectApplicantQuestion.getSelectionPath());
    assertThat(records.get(0).get(multiSelectHeader)).isEqualTo("[toaster, pepper grinder]");
    // Check link for uploaded file
    Question fileuploadQuestion =
        testQuestionBank.getSampleQuestionsForAllTypes().get(QuestionType.FILEUPLOAD);
    FileUploadQuestion fileuploadApplicantQuestion =
        getApplicantQuestion(fileuploadQuestion.getQuestionDefinition()).createFileUploadQuestion();
    String fileKeyHeader =
        ExporterService.pathToHeader(fileuploadApplicantQuestion.getFileKeyPath());
    assertThat(records.get(0).get(fileKeyHeader)).isEqualTo("/admin/programs/1/files/my-file-key");
  }

  @Test
  public void useExporterService() throws Exception {
    // Define the program
    Question nameQuestion = testQuestionBank.applicantName();
    Question colorQuestion = testQuestionBank.applicantFavoriteColor();
    Question householdMembersQuestion = testQuestionBank.applicantHouseholdMembers();
    Question hmNameQuestion = testQuestionBank.applicantHouseholdMemberName();
    Question hmJobsQuestion = testQuestionBank.applicantHouseholdMemberJobs();
    Question hmJobIncomeQuestion = testQuestionBank.applicantHouseholdMemberJobIncome();
    Program program =
        ProgramBuilder.newActiveProgram()
            .withBlock()
            .withQuestions(nameQuestion, colorQuestion)
            .withBlock()
            .withQuestion(householdMembersQuestion)
            .withRepeatedBlock()
            .withQuestion(hmNameQuestion)
            .withAnotherRepeatedBlock()
            .withQuestion(hmJobsQuestion)
            .withRepeatedBlock()
            .withQuestion(hmJobIncomeQuestion)
            .build();
    ExporterService exporterService = instanceOf(ExporterService.class);

    // First applicant has two household members, and the second one has one job.
    Applicant firstApplicant = new Applicant();
    QuestionAnswerer.answerNameQuestion(
        firstApplicant.getApplicantData(),
        ApplicantData.APPLICANT_PATH.join(
            nameQuestion.getQuestionDefinition().getQuestionPathSegment()),
        "Jane",
        "",
        "Doe");
    QuestionAnswerer.answerTextQuestion(
        firstApplicant.getApplicantData(),
        ApplicantData.APPLICANT_PATH.join(
            colorQuestion.getQuestionDefinition().getQuestionPathSegment()),
        "coquelicot");
    Path hmPath =
        ApplicantData.APPLICANT_PATH.join(
            householdMembersQuestion.getQuestionDefinition().getQuestionPathSegment());
    QuestionAnswerer.answerEnumeratorQuestion(
        firstApplicant.getApplicantData(), hmPath, ImmutableList.of("Anne", "Bailey"));
    QuestionAnswerer.answerNameQuestion(
        firstApplicant.getApplicantData(),
        hmPath.atIndex(0).join(hmNameQuestion.getQuestionDefinition().getQuestionPathSegment()),
        "Anne",
        "",
        "Anderson");
    QuestionAnswerer.answerNameQuestion(
        firstApplicant.getApplicantData(),
        hmPath.atIndex(1).join(hmNameQuestion.getQuestionDefinition().getQuestionPathSegment()),
        "Bailey",
        "",
        "Bailerson");
    String hmJobPathSegment = hmJobsQuestion.getQuestionDefinition().getQuestionPathSegment();
    QuestionAnswerer.answerEnumeratorQuestion(
        firstApplicant.getApplicantData(),
        hmPath.atIndex(1).join(hmJobPathSegment),
        ImmutableList.of("Bailey's job"));
    QuestionAnswerer.answerNumberQuestion(
        firstApplicant.getApplicantData(),
        hmPath
            .atIndex(1)
            .join(hmJobPathSegment)
            .atIndex(0)
            .join(hmJobIncomeQuestion.getQuestionDefinition().getQuestionPathSegment()),
        100);
    firstApplicant.save();
    Application firstApplication = new Application(firstApplicant, program, LifecycleStage.ACTIVE);
    firstApplication.save();

    // Second applicant has one household member that has two jobs.
    Applicant secondApplicant = new Applicant();
    QuestionAnswerer.answerNameQuestion(
        secondApplicant.getApplicantData(),
        ApplicantData.APPLICANT_PATH.join(
            nameQuestion.getQuestionDefinition().getQuestionPathSegment()),
        "John",
        "",
        "Doe");
    QuestionAnswerer.answerTextQuestion(
        secondApplicant.getApplicantData(),
        ApplicantData.APPLICANT_PATH.join(
            colorQuestion.getQuestionDefinition().getQuestionPathSegment()),
        "brown");
    QuestionAnswerer.answerEnumeratorQuestion(
        secondApplicant.getApplicantData(), hmPath, ImmutableList.of("James"));
    QuestionAnswerer.answerNameQuestion(
        secondApplicant.getApplicantData(),
        hmPath.atIndex(0).join(hmNameQuestion.getQuestionDefinition().getQuestionPathSegment()),
        "James",
        "",
        "Jameson");
    QuestionAnswerer.answerEnumeratorQuestion(
        secondApplicant.getApplicantData(),
        hmPath.atIndex(0).join(hmJobPathSegment),
        ImmutableList.of("James' first job", "James' second job", "James' third job"));
    QuestionAnswerer.answerNumberQuestion(
        secondApplicant.getApplicantData(),
        hmPath
            .atIndex(0)
            .join(hmJobPathSegment)
            .atIndex(0)
            .join(hmJobIncomeQuestion.getQuestionDefinition().getQuestionPathSegment()),
        111);
    QuestionAnswerer.answerNumberQuestion(
        secondApplicant.getApplicantData(),
        hmPath
            .atIndex(0)
            .join(hmJobPathSegment)
            .atIndex(1)
            .join(hmJobIncomeQuestion.getQuestionDefinition().getQuestionPathSegment()),
        222);
    QuestionAnswerer.answerNumberQuestion(
        secondApplicant.getApplicantData(),
        hmPath
            .atIndex(0)
            .join(hmJobPathSegment)
            .atIndex(2)
            .join(hmJobIncomeQuestion.getQuestionDefinition().getQuestionPathSegment()),
        333);
    secondApplicant.save();
    Application secondApplication =
        new Application(secondApplicant, program, LifecycleStage.ACTIVE);
    secondApplication.save();

    // Generate default CSV
    ApplicantService applicantService = instanceOf(ApplicantService.class);
    ExporterFactory exporterFactory = instanceOf(ExporterFactory.class);
    CsvExporter exporter =
        exporterFactory.csvExporter(exporterService.generateDefaultCsvConfig(program.id));
    exporter.export(
        firstApplication,
        applicantService
            .getReadOnlyApplicantProgramService(firstApplication)
            .toCompletableFuture()
            .get(),
        writer);
    exporter.export(
        secondApplication,
        applicantService
            .getReadOnlyApplicantProgramService(secondApplication)
            .toCompletableFuture()
            .get(),
        writer);
    writer.close();

    CSVParser parser =
        CSVParser.parse(
            exporterService.getProgramCsv(program.id), CSVFormat.DEFAULT.withFirstRecordAsHeader());

    assertThat(parser.getHeaderMap())
        .containsExactlyEntriesOf(
            ImmutableMap.<String, Integer>builder()
                .put("ID", 0)
                .put("Applicant language", 1)
                .put("Submit time", 2)
                .put("Submitted by", 3)
                .put("applicant name (first_name)", 4)
                .put("applicant name (last_name)", 5)
                .put("applicant name (middle_name)", 6)
                .put("applicant favorite color (text)", 7)
                .put("applicant household members[0] - household members name (first_name)", 8)
                .put("applicant household members[0] - household members name (last_name)", 9)
                .put("applicant household members[0] - household members name (middle_name)", 10)
                .put("applicant household members[1] - household members name (first_name)", 11)
                .put("applicant household members[1] - household members name (last_name)", 12)
                .put("applicant household members[1] - household members name (middle_name)", 13)
                .put(
                    "applicant household members[0] - household members jobs[0] - household"
                        + " members jobs income (number)",
                    14)
                .put(
                    "applicant household members[0] - household members jobs[1] - household"
                        + " members jobs income (number)",
                    15)
                .put(
                    "applicant household members[0] - household members jobs[2] - household"
                        + " members jobs income (number)",
                    16)
                .put(
                    "applicant household members[1] - household members jobs[0] - household"
                        + " members jobs income (number)",
                    17)
                .build());

    List<CSVRecord> records = parser.getRecords();
    assertThat(records).hasSize(2);
    assertThat(
            records
                .get(0)
                .get(
                    "applicant household members[0] - household members jobs[2] - household"
                        + " members jobs income (number)"))
        .isEqualTo("");
    assertThat(
            records
                .get(0)
                .get(
                    "applicant household members[1] - household members jobs[0] - household"
                        + " members jobs income (number)"))
        .isEqualTo("100");
    assertThat(
            records
                .get(1)
                .get(
                    "applicant household members[0] - household members jobs[2] - household"
                        + " members jobs income (number)"))
        .isEqualTo("333");
    assertThat(
            records
                .get(1)
                .get(
                    "applicant household members[1] - household members jobs[0] - household"
                        + " members jobs income (number)"))
        .isEqualTo("");
  }
}
