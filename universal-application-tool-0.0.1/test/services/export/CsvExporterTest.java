package services.export;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
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
import org.junit.BeforeClass;
import org.junit.Test;
import repository.WithPostgresContainer;
import services.Path;
import services.applicant.ApplicantData;
import services.program.Column;
import services.program.ColumnType;
import services.program.CsvExportConfig;
import services.program.ExportDefinition;
import services.program.ExportEngine;
import support.ProgramBuilder;
import support.QuestionAnswerer;

public class CsvExporterTest extends WithPostgresContainer {
  private static Program fakeProgramWithCsvExport;
  private ImmutableList<Applicant> fakeApplicants;
  private Writer writer;
  private ByteArrayOutputStream inMemoryBytes;

  public static CsvExportConfig createFakeCsvConfig() {
    return CsvExportConfig.builder()
        .addColumn(
            Column.builder()
                .setHeader("first name")
                .setJsonPath(Path.create("$.applicant.applicant_name.first_name"))
                .setColumnType(ColumnType.APPLICANT)
                .build())
        .addColumn(
            Column.builder()
                .setHeader("last name")
                .setJsonPath(Path.create("$.applicant.applicant_name.last_name"))
                .setColumnType(ColumnType.APPLICANT)
                .build())
        .addColumn(
            Column.builder()
                .setHeader("column")
                .setJsonPath(Path.create("$.applicant.column"))
                .setColumnType(ColumnType.APPLICANT)
                .build())
        .addColumn(
            Column.builder()
                .setHeader("multiselect")
                .setJsonPath(Path.create("$.applicant.multiselect.selection"))
                .setColumnType(ColumnType.APPLICANT)
                .build())
        .build();
  }

  @BeforeClass
  public static void createFakeProgram() {
    fakeProgramWithCsvExport =
        ProgramBuilder.newDraftProgram()
            .withExportDefinition(
                ExportDefinition.builder()
                    .setEngine(ExportEngine.CSV)
                    .setCsvConfig(Optional.of(createFakeCsvConfig()))
                    .build())
            .build();
  }

  @Before
  public void createFakeApplicants() {
    Applicant fakeApplicantOne = new Applicant();
    fakeApplicantOne
        .getApplicantData()
        .putString(Path.create("applicant.applicant_name.first_name"), "Alice");
    fakeApplicantOne
        .getApplicantData()
        .putString(Path.create("applicant.applicant_name.last_name"), "Appleton");
    fakeApplicantOne
        .getApplicantData()
        .putString(
            Path.create("applicant.column"), "Some Value \" containing ,,, special characters");
    fakeApplicantOne
        .getApplicantData()
        .putLong(Path.create("applicant.multiselect.selection[0]"), 1L);
    fakeApplicantOne
        .getApplicantData()
        .putLong(Path.create("applicant.multiselect.selection[1]"), 2L);
    fakeApplicantOne
        .getApplicantData()
        .putString(Path.create("applicant.applicant_favorite_color.text"), "fuchsia");
    fakeApplicantOne.save();

    Applicant fakeApplicantTwo = new Applicant();
    fakeApplicantTwo
        .getApplicantData()
        .putString(Path.create("applicant.applicant_name.first_name"), "Bob");
    fakeApplicantTwo
        .getApplicantData()
        .putString(Path.create("applicant.applicant_name.last_name"), "Baker");
    fakeApplicantTwo.getApplicantData().putString(Path.create("applicant.column"), "");
    fakeApplicantTwo
        .getApplicantData()
        .putString(Path.create("applicant.multiselect.selection[0]"), "hello");
    fakeApplicantTwo
        .getApplicantData()
        .putString(Path.create("applicant.applicant_favorite_color.text"), "maroon");
    fakeApplicantTwo.save();
    this.fakeApplicants = ImmutableList.of(fakeApplicantOne, fakeApplicantTwo);
  }

  @Before
  public void createInMemoryWriter() {
    this.inMemoryBytes = new ByteArrayOutputStream();
    this.writer = new OutputStreamWriter(inMemoryBytes, StandardCharsets.UTF_8);
  }

  @Test
  public void fillOutCsv() throws Exception {
    ExporterFactory exporterFactory = instanceOf(ExporterFactory.class);
    CsvExporter exporter = exporterFactory.csvExporter(fakeProgramWithCsvExport);
    for (Applicant applicant : fakeApplicants) {
      Application application =
          new Application(applicant, fakeProgramWithCsvExport, LifecycleStage.ACTIVE);
      exporter.export(application, writer);
    }
    writer.close();

    CSVParser parser =
        CSVParser.parse(
            inMemoryBytes.toString(StandardCharsets.UTF_8),
            CSVFormat.DEFAULT.withFirstRecordAsHeader());
    assertThat(parser.getHeaderMap()).containsEntry("first name", 0);
    assertThat(parser.getHeaderMap()).containsEntry("last name", 1);
    assertThat(parser.getHeaderMap()).containsEntry("column", 2);
    assertThat(parser.getHeaderMap()).containsEntry("multiselect", 3);
    List<CSVRecord> records = parser.getRecords();
    assertThat(records).hasSize(2);
    assertThat(records.get(0).get("first name")).isEqualTo("Alice");
    assertThat(records.get(1).get("last name")).isEqualTo("Baker");
    // Check list for multiselect
    // TODO: export the string values of the selects instead of the IDs
    assertThat(records.get(0).get("multiselect")).isEqualTo("[1, 2]");
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
    ExporterFactory exporterFactory = instanceOf(ExporterFactory.class);
    CsvExporter exporter =
        exporterFactory.csvExporter(exporterService.generateDefaultCsvConfig(program.id));
    exporter.export(firstApplication, writer);
    exporter.export(secondApplication, writer);
    writer.close();

    CSVParser parser =
        CSVParser.parse(
            exporterService.getProgramCsv(program.id), CSVFormat.DEFAULT.withFirstRecordAsHeader());

    assertThat(parser.getHeaderMap())
        .containsExactlyEntriesOf(
            ImmutableMap.<String, Integer>builder()
                .put("ID", 0)
                .put("Submit time", 1)
                .put("Submitted by", 2)
                .put("applicant name (first_name)", 3)
                .put("applicant name (middle_name)", 4)
                .put("applicant name (last_name)", 5)
                .put("applicant favorite color (text)", 6)
                .put("applicant household members[0] - household members name (first_name)", 7)
                .put("applicant household members[0] - household members name (middle_name)", 8)
                .put("applicant household members[0] - household members name (last_name)", 9)
                .put("applicant household members[1] - household members name (first_name)", 10)
                .put("applicant household members[1] - household members name (middle_name)", 11)
                .put("applicant household members[1] - household members name (last_name)", 12)
                .put(
                    "applicant household members[0] - household members jobs[0] - household"
                        + " members jobs income (number)",
                    13)
                .put(
                    "applicant household members[0] - household members jobs[1] - household"
                        + " members jobs income (number)",
                    14)
                .put(
                    "applicant household members[0] - household members jobs[2] - household"
                        + " members jobs income (number)",
                    15)
                .put(
                    "applicant household members[1] - household members jobs[0] - household"
                        + " members jobs income (number)",
                    16)
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
