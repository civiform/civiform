package services.export;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.common.hash.Hashing;
import com.typesafe.config.ConfigFactory;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import models.Question;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.Before;
import org.junit.Test;
import repository.ExportServiceRepository;
import repository.TimeFilter;
import services.DateConverter;
import services.applicant.ApplicantData;
import services.applicant.ApplicantService;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.FileUploadQuestion;
import services.applicant.question.MultiSelectQuestion;
import services.applicant.question.NameQuestion;
import services.applicant.question.PhoneQuestion;
import services.program.ProgramService;
import services.question.QuestionService;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;

public class CsvExporterTest extends AbstractExporterTest {

  private static final CSVFormat DEFAULT_FORMAT = CSVFormat.DEFAULT.builder().setHeader().build();
  private static final String SECRET_SALT = "super secret";
  private static final String EMPTY_VALUE = "";
  CsvExporterService exporterService;

  private ApplicantQuestion getApplicantQuestion(QuestionDefinition questionDefinition) {
    return new ApplicantQuestion(questionDefinition, new ApplicantData(), Optional.empty());
  }

  @Before
  public void setUp() {
    exporterService =
        new CsvExporterService(
            instanceOf(ProgramService.class),
            instanceOf(QuestionService.class),
            instanceOf(ApplicantService.class),
            ConfigFactory.parseMap(ImmutableMap.of("play.http.secret.key", SECRET_SALT)),
            instanceOf(DateConverter.class),
            instanceOf(ExportServiceRepository.class));
  }

  @Test
  public void programCsv_noRepeatedEntities() throws Exception {
    createFakeQuestions();
    createFakeProgram();
    createFakeApplications();

    CSVParser parser =
        CSVParser.parse(exporterService.getProgramCsv(fakeProgram.id), DEFAULT_FORMAT);
    List<CSVRecord> records = parser.getRecords();

    assertThat(records).hasSize(3);
    assertThat(parser.getHeaderNames())
        .containsExactly(
            "Applicant ID",
            "Application ID",
            "Applicant Language",
            "Submit Time",
            "Submitter Type",
            "TI Email",
            "TI Organization",
            "Status",
            "applicant email address (email)",
            "applicant monthly income (currency)",
            "applicant name (first_name)",
            "applicant name (middle_name)",
            "applicant name (last_name)",
            "applicant phone (phone_number)",
            "applicant phone (country_code)",
            "kitchen tools (selections)",
            "number of items applicant can juggle (number)",
            "applicant address (street)",
            "applicant address (line2)",
            "applicant address (city)",
            "applicant address (state)",
            "applicant address (zip)",
            "applicant address (corrected)",
            "applicant address (latitude)",
            "applicant address (longitude)",
            "applicant address (well_known_id)",
            "applicant address (service_area)",
            "applicant birth date (date)",
            "applicant favorite color (text)",
            "applicant favorite season (selection)",
            "applicant file (file_key)",
            "applicant ice cream (selection)",
            "applicant id (id)");

    NameQuestion nameApplicantQuestion =
        getApplicantQuestion(testQuestionBank.applicantName().getQuestionDefinition())
            .createNameQuestion();
    String firstNameHeader =
        CsvExporterService.pathToHeader(nameApplicantQuestion.getFirstNamePath());
    String lastNameHeader =
        CsvExporterService.pathToHeader(nameApplicantQuestion.getLastNamePath());
    Question phoneQuestion =
        testQuestionBank.getSampleQuestionsForAllTypes().get(QuestionType.PHONE);
    PhoneQuestion phoneQuestion1 =
        getApplicantQuestion(phoneQuestion.getQuestionDefinition()).createPhoneQuestion();
    String phoneHeader = CsvExporterService.pathToHeader(phoneQuestion1.getPhoneNumberPath());
    String countryCodeHeader = CsvExporterService.pathToHeader(phoneQuestion1.getCountryCodePath());
    assertThat(records.get(1).get(phoneHeader)).contains("6157571010");
    assertThat(records.get(1).get(countryCodeHeader)).contains("US");

    // Applications should appear most recent first.
    assertThat(records.get(0).get(firstNameHeader)).isEqualTo("Bob");
    assertThat(records.get(1).get(lastNameHeader)).isEqualTo("Appleton");
    assertThat(records.get(0).get("Status")).isEqualTo("");
    assertThat(records.get(1).get("Status")).isEqualTo(STATUS_VALUE);
    assertThat(records.get(0).get("Submitter Type")).isEqualTo("APPLICANT");
    // Check list for multiselect in default locale
    Question checkboxQuestion =
        testQuestionBank.getSampleQuestionsForAllTypes().get(QuestionType.CHECKBOX);
    MultiSelectQuestion multiSelectApplicantQuestion =
        getApplicantQuestion(checkboxQuestion.getQuestionDefinition()).createMultiSelectQuestion();
    String multiSelectHeader =
        CsvExporterService.pathToHeader(multiSelectApplicantQuestion.getSelectionPath());
    assertThat(records.get(1).get(multiSelectHeader)).isEqualTo("[toaster, pepper_grinder]");
    // Check link for uploaded file
    Question fileuploadQuestion =
        testQuestionBank.getSampleQuestionsForAllTypes().get(QuestionType.FILEUPLOAD);
    FileUploadQuestion fileuploadApplicantQuestion =
        getApplicantQuestion(fileuploadQuestion.getQuestionDefinition()).createFileUploadQuestion();
    String fileKeyHeader =
        CsvExporterService.pathToHeader(fileuploadApplicantQuestion.getFileKeyPath());
    assertThat(records.get(1).get(fileKeyHeader))
        .contains(String.format("/admin/programs/%d/files/my-file-key", fakeProgram.id));
  }

  @Test
  public void programCsv_withEligibility() throws Exception {
    createFakeQuestions();
    createFakeProgramWithEligibilityPredicate();

    CSVParser parser =
        CSVParser.parse(
            exporterService.getProgramCsv(fakeProgramWithEligibility.id), DEFAULT_FORMAT);
    List<CSVRecord> records = parser.getRecords();

    assertThat(records).hasSize(3);
    assertThat(parser.getHeaderNames())
        .containsExactly(
            "Applicant ID",
            "Application ID",
            "Applicant Language",
            "Submit Time",
            "Submitter Type",
            "TI Email",
            "TI Organization",
            "Eligibility Status",
            "Status",
            "applicant name (first_name)",
            "applicant name (middle_name)",
            "applicant name (last_name)",
            "applicant favorite color (text)");

    NameQuestion nameApplicantQuestion =
        getApplicantQuestion(testQuestionBank.applicantName().getQuestionDefinition())
            .createNameQuestion();
    String firstNameHeader =
        CsvExporterService.pathToHeader(nameApplicantQuestion.getFirstNamePath());
    // Applications should appear most recent first.
    assertThat(records.get(0).get(firstNameHeader)).isEqualTo("John");
    assertThat(records.get(1).get(firstNameHeader)).isEqualTo("John");
    assertThat(records.get(2).get(firstNameHeader)).isEqualTo("Jane");
    assertThat(records.get(0).get("Eligibility Status")).isEqualTo("Meets eligibility");
    assertThat(records.get(1).get("Eligibility Status")).isEqualTo("Meets eligibility");
    assertThat(records.get(2).get("Eligibility Status")).isEqualTo("Doesn't meet eligibility");
  }

  @Test
  public void createAndSubmitTimes_presentAndInPST() throws Exception {

    createFakeQuestions();
    createFakeProgram();
    createFakeApplications();

    CSVParser parser =
        CSVParser.parse(exporterService.getDemographicsCsv(TimeFilter.EMPTY), DEFAULT_FORMAT);
    CSVRecord firstApplicationRecord = parser.getRecords().get(0);

    assertThat(firstApplicationRecord.get("Create Time")).isEqualTo("2022/04/09 3:07:02 AM PDT");
    assertThat(firstApplicationRecord.get("Submit Time")).isEqualTo("2022/12/09 2:30:30 AM PST");
  }

  @Test
  public void programCsv_noEntities() throws Exception {
    createFakeQuestions();
    createFakeProgram();

    CsvExporterService exporterService = instanceOf(CsvExporterService.class);
    CSVParser parser =
        CSVParser.parse(exporterService.getProgramCsv(fakeProgram.id), DEFAULT_FORMAT);
    List<CSVRecord> records = parser.getRecords();

    assertThat(records).hasSize(0);

    assertThat(parser.getHeaderNames())
        .containsExactly(
            "Applicant ID",
            "Application ID",
            "Applicant Language",
            "Submit Time",
            "Submitter Type",
            "TI Email",
            "TI Organization",
            "Status");
  }

  @Test
  public void demographicsCsv_withRepeatedEntities() throws Exception {
    createFakeQuestions();
    createFakeProgram();
    createFakeApplications();
    createFakeProgramWithEnumeratorAndAnswerQuestions();

    CSVParser parser =
        CSVParser.parse(exporterService.getDemographicsCsv(TimeFilter.EMPTY), DEFAULT_FORMAT);

    assertThat(parser.getHeaderNames())
        .containsExactly(
            "Opaque ID",
            "Program",
            "Submitter Type",
            "TI Email (Opaque)",
            "TI Organization",
            "Create Time",
            "Submit Time",
            "Status");
  }

  @Test
  public void demographicsCsv_noEntities() throws Exception {
    CsvExporterService exporterService = instanceOf(CsvExporterService.class);
    CSVParser parser =
        CSVParser.parse(exporterService.getDemographicsCsv(TimeFilter.EMPTY), DEFAULT_FORMAT);

    assertThat(parser.getHeaderNames())
        .containsExactly(
            "Opaque ID",
            "Program",
            "Submitter Type",
            "TI Email (Opaque)",
            "TI Organization",
            "Create Time",
            "Submit Time",
            "Status");
    assertThat(parser.getRecords()).hasSize(0);
  }

  @Test
  public void programCsv_withRepeatedEntities() throws Exception {
    createFakeQuestions();
    createFakeProgram();
    createFakeApplications();
    createFakeProgramWithEnumeratorAndAnswerQuestions();

    // Generate default CSV
    CSVParser parser =
        CSVParser.parse(
            exporterService.getProgramCsv(fakeProgramWithEnumerator.id), DEFAULT_FORMAT);

    assertThat(parser.getHeaderNames())
        .containsExactly(
            "Applicant ID",
            "Application ID",
            "Applicant Language",
            "Submit Time",
            "Submitter Type",
            "TI Email",
            "TI Organization",
            "Status",
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

  @Test
  public void getProgramCsv_whenSubmitterIsTi_TiFieldsAreSet() throws Exception {
    var fakeProgram = new FakeProgramBuilder().build();
    new FakeApplicationFiller(fakeProgram)
        .byTrustedIntermediary("ti@trusted_intermediaries.org", "TIs Inc.")
        .submit();

    CSVParser parser =
        CSVParser.parse(exporterService.getProgramCsv(fakeProgram.id), DEFAULT_FORMAT);
    List<CSVRecord> records = parser.getRecords();

    assertThat(records.get(0).get("Submitter Type")).isEqualTo("TRUSTED_INTERMEDIARY");
    assertThat(records.get(0).get("TI Email")).isEqualTo("ti@trusted_intermediaries.org");
    assertThat(records.get(0).get("TI Organization")).isEqualTo("TIs Inc.");
  }

  @Test
  public void getProgramCsv_whenSubmitterIsApplicant_TiFieldsAreNotSet() throws Exception {
    var fakeProgram = new FakeProgramBuilder().build();
    new FakeApplicationFiller(fakeProgram).submit();

    CSVParser parser =
        CSVParser.parse(exporterService.getProgramCsv(fakeProgram.id), DEFAULT_FORMAT);
    List<CSVRecord> records = parser.getRecords();

    assertThat(records.get(0).get("Submitter Type")).isEqualTo("APPLICANT");
    assertThat(records.get(0).get("TI Email")).isEqualTo(EMPTY_VALUE);
    assertThat(records.get(0).get("TI Organization")).isEqualTo(EMPTY_VALUE);
  }

  @Test
  public void getDemographicsCsv_whenSubmitterIsTi_TiFieldsAreSet() throws Exception {
    var fakeProgram = new FakeProgramBuilder().build();
    new FakeApplicationFiller(fakeProgram)
        .byTrustedIntermediary("ti@trusted_intermediaries.org", "TIs Inc.")
        .submit();

    CSVParser parser =
        CSVParser.parse(exporterService.getDemographicsCsv(TimeFilter.EMPTY), DEFAULT_FORMAT);
    List<CSVRecord> records = parser.getRecords();

    var expectedHashedEmail =
        Hashing.sha256()
            .newHasher()
            .putString(SECRET_SALT, StandardCharsets.UTF_8)
            .putString("ti@trusted_intermediaries.org", StandardCharsets.UTF_8)
            .hash()
            .toString();

    assertThat(records.get(0).get("Submitter Type")).isEqualTo("TRUSTED_INTERMEDIARY");
    assertThat(records.get(0).get("TI Email (Opaque)")).isEqualTo(expectedHashedEmail);
    assertThat(records.get(0).get("TI Organization")).isEqualTo("TIs Inc.");
  }

  @Test
  public void getDemographicsCsv_whenSubmitterIsApplicant_TiFieldsAreNotSet() throws Exception {
    var fakeProgram = new FakeProgramBuilder().build();
    new FakeApplicationFiller(fakeProgram).submit();

    CSVParser parser =
        CSVParser.parse(exporterService.getDemographicsCsv(TimeFilter.EMPTY), DEFAULT_FORMAT);
    List<CSVRecord> records = parser.getRecords();

    assertThat(records.get(0).get("Submitter Type")).isEqualTo("APPLICANT");
    assertThat(records.get(0).get("TI Email (Opaque)")).isEqualTo(EMPTY_VALUE);
    assertThat(records.get(0).get("TI Organization")).isEqualTo(EMPTY_VALUE);
  }
}
