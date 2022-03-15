package services.export;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Streams;
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
import repository.ResetPostgres;
import services.Path;
import services.applicant.ApplicantData;
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

public class CsvExporterTest extends ResetPostgres {
  private Program fakeProgramWithCsvExport;
  private ImmutableList<Question> fakeQuestions;

  private void createFakeQuestions() {
    this.fakeQuestions =
        testQuestionBank.getSampleQuestionsForAllTypes().values().stream()
            .sorted(Comparator.comparing(question -> question.getQuestionDefinition().getName()))
            .collect(ImmutableList.toImmutableList());
  }

  private ApplicantQuestion getApplicantQuestion(QuestionDefinition questionDefinition) {
    return new ApplicantQuestion(questionDefinition, new ApplicantData(), Optional.empty());
  }

  private CsvExportConfig createFakeCsvConfig() {
    CsvExportConfig.Builder csvExportConfigBuilder = CsvExportConfig.builder();
    fakeQuestions.stream()
        .map(question -> question.getQuestionDefinition())
        .filter(question -> !question.isEnumerator())
        .flatMap(
            question -> getApplicantQuestion(question).getContextualizedScalars().keySet().stream())
        .filter(path -> !Scalar.getMetadataScalarKeys().contains(path.keyName()))
        .forEach(
            path ->
                csvExportConfigBuilder.addColumn(
                    Column.builder()
                        .setHeader(ExporterService.pathToHeader(path))
                        .setJsonPath(path)
                        .setColumnType(ColumnType.APPLICANT_ANSWER)
                        .build()));
    return csvExportConfigBuilder.build();
  }

  private void createFakeProgram() {
    ProgramBuilder fakeProgram = ProgramBuilder.newActiveProgram();
    createFakeQuestions();
    fakeQuestions.forEach(
        question -> fakeProgram.withBlock().withRequiredQuestion(question).build());

    this.fakeProgramWithCsvExport =
        fakeProgram
            .withExportDefinition(
                ExportDefinition.builder()
                    .setEngine(ExportEngine.CSV)
                    .setCsvConfig(Optional.of(createFakeCsvConfig()))
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
      case CURRENCY:
        QuestionAnswerer.answerCurrencyQuestion(applicantDataOne, answerPath, "1,234.56");
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
      case ID:
        QuestionAnswerer.answerIdQuestion(applicantDataOne, answerPath, "012");
        QuestionAnswerer.answerIdQuestion(applicantDataTwo, answerPath, "123");
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
      case STATIC:
        // Do nothing.
        break;
    }
  }

  private void createFakeApplications() {
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
    new Application(fakeApplicantOne, fakeProgramWithCsvExport, LifecycleStage.ACTIVE).save();
    new Application(fakeApplicantOne, fakeProgramWithCsvExport, LifecycleStage.OBSOLETE).save();
    new Application(fakeApplicantOne, fakeProgramWithCsvExport, LifecycleStage.DRAFT).save();
    new Application(fakeApplicantTwo, fakeProgramWithCsvExport, LifecycleStage.ACTIVE).save();
  }

  @Before
  public void setUp() {
    createFakeProgram();
    createFakeApplications();
  }

  @Test
  public void useProgramCsvExport_noRepeatedEntities() throws Exception {
    ExporterService exporterService = instanceOf(ExporterService.class);
    CSVParser parser =
        CSVParser.parse(
            exporterService.getProgramCsv(fakeProgramWithCsvExport.id),
            CSVFormat.DEFAULT.withFirstRecordAsHeader());
    List<CSVRecord> records = parser.getRecords();
    assertThat(records).hasSize(3);
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
                                path -> !Scalar.getMetadataScalarKeys().contains(path.keyName()))),
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
    // Applications should appear most recent first.
    assertThat(records.get(0).get(firstNameHeader)).isEqualTo("Bob");
    assertThat(records.get(1).get(lastNameHeader)).isEqualTo("Appleton");
    // Check list for multiselect in default locale
    Question checkboxQuestion =
        testQuestionBank.getSampleQuestionsForAllTypes().get(QuestionType.CHECKBOX);
    MultiSelectQuestion multiSelectApplicantQuestion =
        getApplicantQuestion(checkboxQuestion.getQuestionDefinition()).createMultiSelectQuestion();
    String multiSelectHeader =
        ExporterService.pathToHeader(multiSelectApplicantQuestion.getSelectionPath());
    assertThat(records.get(1).get(multiSelectHeader)).isEqualTo("[toaster, pepper grinder]");
    // Check link for uploaded file
    Question fileuploadQuestion =
        testQuestionBank.getSampleQuestionsForAllTypes().get(QuestionType.FILEUPLOAD);
    FileUploadQuestion fileuploadApplicantQuestion =
        getApplicantQuestion(fileuploadQuestion.getQuestionDefinition()).createFileUploadQuestion();
    String fileKeyHeader =
        ExporterService.pathToHeader(fileuploadApplicantQuestion.getFileKeyPath());
    assertThat(records.get(1).get(fileKeyHeader))
        .contains(
            String.format("/admin/programs/%d/files/my-file-key", fakeProgramWithCsvExport.id));
  }

  @Test
  public void useDefaultCsvConfig_withRepeatedEntities() throws Exception {
    // Define the program
    Question nameQuestion = testQuestionBank.applicantName();
    Question colorQuestion = testQuestionBank.applicantFavoriteColor();
    Question monthlyIncomeQuestion = testQuestionBank.applicantMonthlyIncome();
    Question householdMembersQuestion = testQuestionBank.applicantHouseholdMembers();
    Question hmNameQuestion = testQuestionBank.applicantHouseholdMemberName();
    Question hmJobsQuestion = testQuestionBank.applicantHouseholdMemberJobs();
    Question hmNumberDaysWorksQuestion = testQuestionBank.applicantHouseholdMemberDaysWorked();
    Program program =
        ProgramBuilder.newActiveProgram()
            .withBlock()
            .withRequiredQuestions(nameQuestion, colorQuestion, monthlyIncomeQuestion)
            .withBlock()
            .withRequiredQuestion(householdMembersQuestion)
            .withRepeatedBlock()
            .withRequiredQuestion(hmNameQuestion)
            .withAnotherRepeatedBlock()
            .withRequiredQuestion(hmJobsQuestion)
            .withRepeatedBlock()
            .withRequiredQuestion(hmNumberDaysWorksQuestion)
            .build();

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
            .join(hmNumberDaysWorksQuestion.getQuestionDefinition().getQuestionPathSegment()),
        100);
    firstApplicant.save();
    Application firstApplication = new Application(firstApplicant, program, LifecycleStage.ACTIVE);
    firstApplication.setSubmitTimeToNow();
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
            .join(hmNumberDaysWorksQuestion.getQuestionDefinition().getQuestionPathSegment()),
        111);
    QuestionAnswerer.answerNumberQuestion(
        secondApplicant.getApplicantData(),
        hmPath
            .atIndex(0)
            .join(hmJobPathSegment)
            .atIndex(1)
            .join(hmNumberDaysWorksQuestion.getQuestionDefinition().getQuestionPathSegment()),
        222);
    QuestionAnswerer.answerNumberQuestion(
        secondApplicant.getApplicantData(),
        hmPath
            .atIndex(0)
            .join(hmJobPathSegment)
            .atIndex(2)
            .join(hmNumberDaysWorksQuestion.getQuestionDefinition().getQuestionPathSegment()),
        333);
    secondApplicant.save();
    Application secondApplication =
        new Application(secondApplicant, program, LifecycleStage.ACTIVE);
    secondApplication.setSubmitTimeToNow();
    secondApplication.save();

    Application thirdApplication =
        new Application(secondApplicant, program, LifecycleStage.OBSOLETE);
    thirdApplication.setSubmitTimeToNow();
    thirdApplication.save();

    // Generate default CSV
    ExporterService exporterService = instanceOf(ExporterService.class);
    CSVParser parser =
        CSVParser.parse(
            exporterService.getProgramCsv(program.id), CSVFormat.DEFAULT.withFirstRecordAsHeader());

    int id = 0;
    assertThat(parser.getHeaderMap())
        .containsExactlyEntriesOf(
            ImmutableMap.<String, Integer>builder()
                .put("Applicant ID", id++)
                .put("Application ID", id++)
                .put("Applicant language", id++)
                .put("Submit time", id++)
                .put("Submitted by", id++)
                .put("applicant name (first_name)", id++)
                .put("applicant name (middle_name)", id++)
                .put("applicant name (last_name)", id++)
                .put("applicant favorite color (text)", id++)
                .put("applicant monthly income (currency)", id++)
                .put("applicant household members[0] - household members name (first_name)", id++)
                .put("applicant household members[0] - household members name (middle_name)", id++)
                .put("applicant household members[0] - household members name (last_name)", id++)
                .put("applicant household members[1] - household members name (first_name)", id++)
                .put("applicant household members[1] - household members name (middle_name)", id++)
                .put("applicant household members[1] - household members name (last_name)", id++)
                .put(
                    "applicant household members[0] - household members jobs[0] - household"
                        + " members days worked (number)",
                    id++)
                .put(
                    "applicant household members[0] - household members jobs[1] - household"
                        + " members days worked (number)",
                    id++)
                .put(
                    "applicant household members[0] - household members jobs[2] - household"
                        + " members days worked (number)",
                    id++)
                .put(
                    "applicant household members[1] - household members jobs[0] - household"
                        + " members days worked (number)",
                    id++)
                .build());

    List<CSVRecord> records = parser.getRecords();
    assertThat(records).hasSize(3);

    // Records should be ordered most recent first.
    assertThat(
            records
                .get(0)
                .get(
                    "applicant household members[0] - household members jobs[2] - household"
                        + " members days worked (number)"))
        .isEqualTo("333");
    assertThat(
            records
                .get(1)
                .get(
                    "applicant household members[0] - household members jobs[2] - household"
                        + " members days worked (number)"))
        .isEqualTo("333");
    assertThat(
            records
                .get(1)
                .get(
                    "applicant household members[1] - household members jobs[0] - household"
                        + " members days worked (number)"))
        .isEqualTo("");
    assertThat(
            records
                .get(2)
                .get(
                    "applicant household members[0] - household members jobs[2] - household"
                        + " members days worked (number)"))
        .isEqualTo("");
    assertThat(
            records
                .get(2)
                .get(
                    "applicant household members[1] - household members jobs[0] - household"
                        + " members days worked (number)"))
        .isEqualTo("100");
  }
}
