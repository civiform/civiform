package services.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import models.Question;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.Test;
import repository.TimeFilter;
import services.applicant.ApplicantData;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.FileUploadQuestion;
import services.applicant.question.MultiSelectQuestion;
import services.applicant.question.NameQuestion;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;
import support.ProgramBuilder;

public class CsvExporterTest extends AbstractExporterTest {

  private static final CSVFormat DEFAULT_FORMAT = CSVFormat.DEFAULT.builder().setHeader().build();

  private ApplicantQuestion getApplicantQuestion(QuestionDefinition questionDefinition) {
    return new ApplicantQuestion(questionDefinition, new ApplicantData(), Optional.empty());
  }

  @Override
  protected void createFakeProgram() {
    ProgramBuilder fakeProgram = ProgramBuilder.newActiveProgram();

    createFakeQuestions();
    fakeQuestions.forEach(
        question -> fakeProgram.withBlock().withRequiredQuestion(question).build());

    this.fakeProgram = fakeProgram.build();
  }

  @Test
  public void programCsv_noRepeatedEnties() throws Exception {
    createFakeProgram();
    createFakeApplications();

    ExporterService exporterService = instanceOf(ExporterService.class);
    CSVParser parser =
        CSVParser.parse(exporterService.getProgramCsv(fakeProgram.id), DEFAULT_FORMAT);
    List<CSVRecord> records = parser.getRecords();

    assertThat(records).hasSize(3);
    assertThat(parser.getHeaderNames())
        .containsExactly(
            "Applicant ID",
            "Application ID",
            "Applicant language",
            "Submit time",
            "Submitted by",
            "applicant email address (email)",
            "applicant name (first_name)",
            "applicant name (middle_name)",
            "applicant name (last_name)",
            "kitchen tools (selections)",
            "number of items applicant can juggle (number)",
            "radio (selection)",
            "applicant address (street)",
            "applicant address (line2)",
            "applicant address (city)",
            "applicant address (state)",
            "applicant address (zip)",
            "applicant birth date (date)",
            "applicant favorite color (text)",
            "applicant file (file_key)",
            "applicant ice cream (selection)",
            "applicant id (id)",
            "applicant monthly income (currency)");

    NameQuestion nameApplicantQuestion =
        getApplicantQuestion(testQuestionBank.applicantName().getQuestionDefinition())
            .createNameQuestion();
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
        .contains(String.format("/admin/programs/%d/files/my-file-key", fakeProgram.id));
  }

  @Test
  public void programCsv_noEntities() throws Exception {
    createFakeProgram();

    ExporterService exporterService = instanceOf(ExporterService.class);
    CSVParser parser =
        CSVParser.parse(exporterService.getProgramCsv(fakeProgram.id), DEFAULT_FORMAT);
    List<CSVRecord> records = parser.getRecords();

    assertThat(records).hasSize(0);

    // No applications means there are no answers to add columns for.
    assertThat(parser.getHeaderNames())
        .containsExactly(
            "Applicant ID", "Application ID", "Applicant language", "Submit time", "Submitted by");
  }

  @Test
  public void demographicsCsv_withRepeatedEntities() throws Exception {
    createFakeProgram();
    createFakeApplications();
    createFakeProgramWithEnumerator();

    ExporterService exporterService = instanceOf(ExporterService.class);
    CSVParser parser =
        CSVParser.parse(exporterService.getDemographicsCsv(TimeFilter.EMPTY), DEFAULT_FORMAT);

    assertThat(parser.getHeaderNames())
        .containsExactly(
            "Opaque ID",
            "Program",
            "Submitter Email (Opaque)",
            "TI Organization",
            "Create time",
            "Submit time");
  }

  @Test
  public void demographicsCsv_noEntities() throws Exception {
    ExporterService exporterService = instanceOf(ExporterService.class);
    CSVParser parser =
        CSVParser.parse(exporterService.getDemographicsCsv(TimeFilter.EMPTY), DEFAULT_FORMAT);

    assertThat(parser.getHeaderNames())
        .containsExactly(
            "Opaque ID",
            "Program",
            "Submitter Email (Opaque)",
            "TI Organization",
            "Create time",
            "Submit time");
    assertThat(parser.getRecords()).hasSize(0);
  }

  @Test
  public void programCsv_withRepeatedEntities() throws Exception {
    createFakeProgram();
    createFakeApplications();
    createFakeProgramWithEnumerator();

    // Generate default CSV
    ExporterService exporterService = instanceOf(ExporterService.class);
    CSVParser parser =
        CSVParser.parse(
            exporterService.getProgramCsv(fakeProgramWithEnumerator.id), DEFAULT_FORMAT);

    assertThat(parser.getHeaderNames())
        .containsExactly(
            "Applicant ID",
            "Application ID",
            "Applicant language",
            "Submit time",
            "Submitted by",
            "applicant name (first_name)",
            "applicant name (middle_name)",
            "applicant name (last_name)",
            "applicant favorite color (text)",
            "applicant monthly income (currency)",
            "applicant household members[0] - household members name (first_name)",
            "applicant household members[0] - household members name (middle_name)",
            "applicant household members[0] - household members name (last_name)",
            "applicant household members[1] - household members name (first_name)",
            "applicant household members[1] - household members name (middle_name)",
            "applicant household members[1] - household members name (last_name)",
            "applicant household members[0] - household members jobs[0] - household"
                + " members days worked (number)",
            "applicant household members[0] - household members jobs[1] - household"
                + " members days worked (number)",
            "applicant household members[0] - household members jobs[2] - household"
                + " members days worked (number)",
            "applicant household members[1] - household members jobs[0] - household"
                + " members days worked (number)");

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
