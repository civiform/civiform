package services.export;

import static org.assertj.core.api.Assertions.assertThat;
import static play.api.test.Helpers.testServerPort;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.hash.Hashing;
import com.typesafe.config.ConfigFactory;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;
import models.AccountModel;
import models.ApplicantModel;
import models.ApplicationModel;
import models.LifecycleStage;
import models.ProgramModel;
import models.QuestionTag;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.Before;
import org.junit.Test;
import repository.SubmittedApplicationFilter;
import repository.TimeFilter;
import repository.VersionRepository;
import services.DateConverter;
import services.LocalizedStrings;
import services.applicant.ApplicantService;
import services.application.ApplicationEventDetails.StatusEvent;
import services.geo.CorrectedAddressState;
import services.geo.ServiceAreaInclusion;
import services.geo.ServiceAreaState;
import services.program.ProgramService;
import services.question.QuestionOption;
import services.question.QuestionService;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionDefinitionConfig;
import services.question.types.TextQuestionDefinition;

public class CsvExporterServiceTest extends AbstractExporterTest {

  private static final CSVFormat DEFAULT_FORMAT = CSVFormat.DEFAULT.builder().setHeader().build();
  private static final String SECRET_SALT = "super secret";
  private static final String BASE_URL = String.format("http://localhost:%d", testServerPort());
  private static final ImmutableList<String> metadataHeaders =
      ImmutableList.of(
          "Applicant ID",
          "Application ID",
          "Applicant Language",
          "Submit Time",
          "Submitter Type",
          "TI Email",
          "TI Organization",
          "Status",
          "Admin Note",
          "Status Create Time");
  private static final ImmutableList<String> demographicMetadataHeaders =
      ImmutableList.of(
          "Opaque ID",
          "Program",
          "Submitter Type",
          "TI Email (Opaque)",
          "TI Organization",
          "Create Time",
          "Submit Time",
          "Status");
  CsvExporterService exporterService;
  private QuestionService questionService;
  private VersionRepository versionRepository;

  @Before
  public void setUp() {
    questionService = instanceOf(QuestionService.class);
    versionRepository = instanceOf(VersionRepository.class);
    exporterService =
        new CsvExporterService(
            instanceOf(ProgramService.class),
            instanceOf(QuestionService.class),
            instanceOf(ApplicantService.class),
            ConfigFactory.parseMap(ImmutableMap.of("play.http.secret.key", SECRET_SALT)),
            instanceOf(DateConverter.class),
            instanceOf(CsvColumnFactory.class));
  }

  @Test
  public void getProgramAllVersionsCsv_metadataColumns() throws Exception {
    // Times are expected to be exported in instance local time, so we choose
    // other timezones here to ensure they're updated.
    Instant submitTime = Instant.parse("2015-10-22T08:28:02-08:00");
    ProgramModel fakeProgram = FakeProgramBuilder.newActiveProgram("fake-program").build();
    ApplicantModel applicant = resourceCreator.insertApplicantWithAccount();
    applicant.getApplicantData().setPreferredLocale(Locale.forLanguageTag("ko"));
    applicant.save();
    ApplicationModel application =
        FakeApplicationFiller.newFillerFor(fakeProgram, applicant)
            .atSubmitTime(submitTime)
            .submit()
            .getApplication();

    CSVRecord record = getParsedRecords(fakeProgram.id).get(0);

    assertThat(record.getParser().getHeaderNames())
        .containsExactly(
            "Applicant ID",
            "Application ID",
            "Applicant Language",
            "Submit Time",
            "Submitter Type",
            "TI Email",
            "TI Organization",
            "Status",
            "Admin Note",
            "Status Create Time");

    assertThat(record.get("Applicant ID")).isEqualTo(applicant.id.toString());
    assertThat(record.get("Application ID")).isEqualTo(application.id.toString());
    assertThat(record.get("Applicant Language")).isEqualTo("ko");
    assertThat(record.get("Submit Time")).isEqualTo("2015/10/22 09:28:02 AM PDT");
    assertThat(record.get("Submitter Type")).isEqualTo("APPLICANT");
    // TI fields tested separately.
    assertThat(record.get("TI Email")).isEmpty();
    assertThat(record.get("TI Organization")).isEmpty();
    // Status field tested separately.
    assertThat(record.get("Status")).isEmpty();
    assertThat(record.get("Status Create Time")).isEqualTo("2022/04/09 03:07:02 AM PDT");
  }

  @Test
  public void getDemographicsCsv_metadataColumns() throws Exception {
    // Times are expected to be exported in instance local time, so we choose
    // multiple timezones here to verify they're updated.
    Instant createTime = Instant.parse("2015-10-21T05:28:02-06:00");
    Instant submitTime = Instant.parse("2015-10-22T08:28:02-08:00");
    ProgramModel fakeProgram = FakeProgramBuilder.newActiveProgram("fake-program").build();
    ApplicantModel applicant = resourceCreator.insertApplicantWithAccount();
    applicant.getApplicantData().setPreferredLocale(Locale.forLanguageTag("ko"));
    applicant.save();
    FakeApplicationFiller.newFillerFor(fakeProgram, applicant)
        .atCreateTime(createTime)
        .atSubmitTime(submitTime)
        .submit()
        .getApplication();

    CSVRecord record = getParsedRecordsFromDemographicCsv().get(0);

    assertThat(record.getParser().getHeaderNames())
        .containsExactly(
            "Opaque ID",
            "Program",
            "Submitter Type",
            "TI Email (Opaque)",
            "TI Organization",
            "Create Time",
            "Submit Time",
            "Status");

    assertThat(record.get("Opaque ID")).isEqualTo(fakeHash(applicant.id));
    assertThat(record.get("Program")).isEqualTo("fake-program");
    assertThat(record.get("Submitter Type")).isEqualTo("APPLICANT");
    // TI fields tested separately.
    assertThat(record.get("TI Email (Opaque)")).isEmpty();
    assertThat(record.get("TI Organization")).isEmpty();
    assertThat(record.get("Create Time")).isEqualTo("2015/10/21 04:28:02 AM PDT");
    assertThat(record.get("Submit Time")).isEqualTo("2015/10/22 09:28:02 AM PDT");
    // Status field tested separately.
    assertThat(record.get("Status")).isEmpty();
  }

  @Test
  public void getProgramAllVersionsCsv_whenApplicationHasStatus_statusColumnIsPopulatedCorrectly()
      throws Exception {
    String status = "approved";
    AccountModel admin = resourceCreator.insertAccount();
    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram().withStatuses(ImmutableList.of(status)).build();

    FakeApplicationFiller fakeApplicationFillerA =
        FakeApplicationFiller.newFillerFor(fakeProgram).submit();
    ApplicationModel fakeApplicationA = fakeApplicationFillerA.getApplication();

    programAdminApplicationService.setStatus(
        fakeApplicationA.id,
        fakeProgram.getProgramDefinition(),
        Optional.empty(),
        StatusEvent.builder().setEmailSent(false).setStatusText(status).build(),
        admin);

    FakeApplicationFiller.newFillerFor(fakeProgram).submit();

    ImmutableList<CSVRecord> records = getParsedRecords(fakeProgram.id);
    // results are in reverse order from submission
    assertThat(records.get(0).get("Status")).isEmpty();
    assertThat(records.get(1).get("Status")).isEqualTo("approved");

    // status create time checks
    assertThat(records.get(1).get("Status Create Time")).isNotEmpty();
    // the default status create time should be overridden
    assertThat(records.get(1).get("Status Create Time")).isNotEqualTo("2022/04/09 03:07:02 AM PDT");
  }

  @Test
  public void getDemographicsCsv_whenApplicationHasStatus_statusColumnIsPopulatedCorrectly()
      throws Exception {
    String status = "approved";
    AccountModel admin = resourceCreator.insertAccount();
    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram().withStatuses(ImmutableList.of(status)).build();

    FakeApplicationFiller fakeApplicationFillerA =
        FakeApplicationFiller.newFillerFor(fakeProgram).submit();
    ApplicationModel fakeApplicationA = fakeApplicationFillerA.getApplication();
    programAdminApplicationService.setStatus(
        fakeApplicationA.id,
        fakeProgram.getProgramDefinition(),
        Optional.empty(),
        StatusEvent.builder().setEmailSent(false).setStatusText(status).build(),
        admin);

    FakeApplicationFiller.newFillerFor(fakeProgram).submit();

    ImmutableList<CSVRecord> records = getParsedRecordsFromDemographicCsv();

    // results are in reverse order from submission
    assertThat(records.get(0).get("Status")).isEqualTo("approved");
    assertThat(records.get(1).get("Status")).isEmpty();
  }

  @Test
  public void getProgramAllVersionsCsv_eligibilityColumnsIsPopulatedCorrectly() throws Exception {
    createFakeQuestions();
    // Create program and apply without eligibility enabled
    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram("Fake Program")
            .withQuestion(testQuestionBank.nameApplicantName())
            .withQuestion(testQuestionBank.textApplicantFavoriteColor())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerTextQuestion(
            testQuestionBank.textApplicantFavoriteColor(), "blue") // eligible answer
        .answerNameQuestion(
            testQuestionBank.nameApplicantName(), "Kylie", "Pre-Eligibility", "Minogue", "")
        .submit();

    // Enable eligibility and apply more times
    ProgramModel fakeProgramWithEligibility =
        createFakeProgramWithEligibilityPredicate("Fake Program");

    FakeApplicationFiller.newFillerFor(fakeProgramWithEligibility)
        .answerTextQuestion(
            testQuestionBank.textApplicantFavoriteColor(), "blue") // eligible answer
        .answerNameQuestion(
            testQuestionBank.nameApplicantName(), "John", "Jacob", "Jingleheimer-Schmidt", "")
        .submit();
    FakeApplicationFiller.newFillerFor(fakeProgramWithEligibility)
        .answerTextQuestion(
            testQuestionBank.textApplicantFavoriteColor(), "red") // in-eligible answer
        .answerNameQuestion(testQuestionBank.nameApplicantName(), "His name", "is my", "name", "II")
        .submit();

    ImmutableList<CSVRecord> records = getParsedRecords(fakeProgramWithEligibility.id);

    assertThat(records).hasSize(3);
    assertThat(records.get(0).getParser().getHeaderNames())
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
            "applicant favorite color (text)",
            "applicant name (first_name)",
            "applicant name (middle_name)",
            "applicant name (last_name)",
            "applicant name (suffix)",
            "Admin Note",
            "Status Create Time");

    // Applications should appear most recent first.
    assertThat(records.get(0).get("applicant name (first_name)")).isEqualTo("His name");
    assertThat(records.get(0).get("Eligibility Status")).isEqualTo("Doesn't meet eligibility");
    assertThat(records.get(1).get("applicant name (first_name)")).isEqualTo("John");
    assertThat(records.get(1).get("Eligibility Status")).isEqualTo("Meets eligibility");
    assertThat(records.get(2).get("applicant name (first_name)")).isEqualTo("Kylie");
    // Empty if eligibility wasn't enabled at the time of submission.
    assertThat(records.get(2).get("Eligibility Status")).isEmpty();
  }

  @Test
  public void getProgramAllVersionsCsv_createAndSubmitTimes_presentAndInInstanceLocalTime()
      throws Exception {

    createFakeQuestions();
    createFakeProgram();
    createFakeApplications();

    CSVParser parser =
        CSVParser.parse(exporterService.getDemographicsCsv(TimeFilter.EMPTY), DEFAULT_FORMAT);
    CSVRecord firstApplicationRecord = parser.getRecords().get(0);

    assertThat(firstApplicationRecord.get("Create Time")).isEqualTo("2022/04/09 03:07:02 AM PDT");
    assertThat(firstApplicationRecord.get("Submit Time")).isEqualTo("2022/12/09 02:30:30 AM PST");
  }

  @Test
  public void getProgramAllVersionsCsv_noApplications() throws Exception {
    createFakeQuestions();
    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.idApplicantId())
            .build();

    CSVParser parser =
        CSVParser.parse(
            exporterService.getProgramAllVersionsCsv(
                fakeProgram.id, SubmittedApplicationFilter.EMPTY),
            DEFAULT_FORMAT);

    assertThat(parser.getRecords()).hasSize(0);
    // If there are no applications in the result set, then no question columns are built.
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
            "Admin Note",
            "Status Create Time");
  }

  @Test
  public void getDemographicsCsv_noApplications() throws Exception {
    createFakeQuestions();
    testQuestionBank.idApplicantId().addTag(QuestionTag.DEMOGRAPHIC_PII);
    testQuestionBank.idApplicantId().save();

    FakeProgramBuilder.newActiveProgram().withQuestion(testQuestionBank.idApplicantId()).build();

    CSVParser parser =
        CSVParser.parse(exporterService.getDemographicsCsv(TimeFilter.EMPTY), DEFAULT_FORMAT);

    assertThat(parser.getRecords()).hasSize(0);
    assertThat(parser.getHeaderNames())
        .containsExactly(
            "Opaque ID",
            "Program",
            "Submitter Type",
            "TI Email (Opaque)",
            "TI Organization",
            "Create Time",
            "Submit Time",
            "Status",
            // Questions without answers are included in the demographic CSV
            "applicant id (id)");
  }

  @Test
  public void getProgramAllVersionsCsv_noAnswersToQuestion_columnIsStillInResult()
      throws Exception {
    createFakeQuestions();
    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.idApplicantId())
            .build();

    FakeApplicationFiller.newFillerFor(fakeProgram).submit();

    CSVParser parser =
        CSVParser.parse(
            exporterService.getProgramAllVersionsCsv(
                fakeProgram.id, SubmittedApplicationFilter.EMPTY),
            DEFAULT_FORMAT);

    assertThat(parser.getRecords()).hasSize(1);
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
            // Unanswered questions should still have a column
            "applicant id (id)",
            "Admin Note",
            "Status Create Time");
  }

  @Test
  public void getDemographicsCsv_noAnswersToQuestion_columnIsStillInResult() throws Exception {
    createFakeQuestions();
    testQuestionBank.idApplicantId().addTag(QuestionTag.DEMOGRAPHIC_PII);
    testQuestionBank.idApplicantId().save();

    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.idApplicantId())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram).submit();

    CSVParser parser =
        CSVParser.parse(exporterService.getDemographicsCsv(TimeFilter.EMPTY), DEFAULT_FORMAT);

    assertThat(parser.getRecords()).hasSize(1);
    assertThat(parser.getHeaderNames())
        .containsExactly(
            "Opaque ID",
            "Program",
            "Submitter Type",
            "TI Email (Opaque)",
            "TI Organization",
            "Create Time",
            "Submit Time",
            "Status",
            // Questions without answers are included in the demographic CSV
            "applicant id (id)");
  }

  @Test
  public void getProgramAllVersionsCsv_questionColumnsOrderedLexicographicallyByPath()
      throws Exception {
    createFakeQuestions();
    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.phoneApplicantPhone())
            .withQuestion(testQuestionBank.dateApplicantBirthdate())
            .withQuestion(testQuestionBank.dropdownApplicantIceCream())
            .withHouseholdMembersEnumeratorQuestion()
            .withHouseholdMembersRepeatedQuestion(testQuestionBank.idRepeatedHouseholdMemberId())
            .withHouseholdMembersJobsNestedEnumeratorQuestion()
            .build();

    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerPhoneQuestion(testQuestionBank.phoneApplicantPhone(), "US", "5558675309")
        .answerDateQuestion(testQuestionBank.dateApplicantBirthdate(), "2015-10-21")
        .answerEnumeratorQuestion(
            ImmutableList.of(
                "alpha", "beta", "gamma", "delta", "epsilon", "zeta", "eta", "theta", "iota",
                "kappa", "lambda"))
        .answerIdQuestion(
            testQuestionBank.idRepeatedHouseholdMemberId(), "alpha", Integer.toString(0))
        .answerIdQuestion(
            testQuestionBank.idRepeatedHouseholdMemberId(), "beta", Integer.toString(1))
        .answerIdQuestion(
            testQuestionBank.idRepeatedHouseholdMemberId(), "gamma", Integer.toString(2))
        .answerIdQuestion(
            testQuestionBank.idRepeatedHouseholdMemberId(), "delta", Integer.toString(3))
        .answerIdQuestion(
            testQuestionBank.idRepeatedHouseholdMemberId(), "epsilon", Integer.toString(4))
        .answerIdQuestion(
            testQuestionBank.idRepeatedHouseholdMemberId(), "zeta", Integer.toString(5))
        .answerIdQuestion(
            testQuestionBank.idRepeatedHouseholdMemberId(), "eta", Integer.toString(6))
        .answerIdQuestion(
            testQuestionBank.idRepeatedHouseholdMemberId(), "theta", Integer.toString(7))
        .answerIdQuestion(
            testQuestionBank.idRepeatedHouseholdMemberId(), "iota", Integer.toString(8))
        .answerIdQuestion(
            testQuestionBank.idRepeatedHouseholdMemberId(), "kappa", Integer.toString(9))
        .answerIdQuestion(
            testQuestionBank.idRepeatedHouseholdMemberId(), "lambda", Integer.toString(10))
        .answerNestedEnumeratorQuestion("beta", ImmutableList.of("zéro", "un"))
        .answerNestedRepeatedNumberQuestion("beta", "zéro", 0)
        .answerNestedRepeatedNumberQuestion("beta", "un", 1)
        .answerNestedEnumeratorQuestion(
            "lambda",
            ImmutableList.of(
                "cero", "uno", "dos", "tres", "quatro", "cinco", "seis", "siete", "ocho", "nueve",
                "diez"))
        .answerNestedRepeatedNumberQuestion("lambda", "uno", 1)
        .answerNestedRepeatedNumberQuestion("lambda", "dos", 2)
        .answerNestedRepeatedNumberQuestion("lambda", "tres", 3)
        .answerNestedRepeatedNumberQuestion("lambda", "quatro", 4)
        .answerNestedRepeatedNumberQuestion("lambda", "cinco", 5)
        .answerNestedRepeatedNumberQuestion("lambda", "seis", 6)
        .answerNestedRepeatedNumberQuestion("lambda", "siete", 7)
        .answerNestedRepeatedNumberQuestion("lambda", "ocho", 8)
        .answerNestedRepeatedNumberQuestion("lambda", "nueve", 9)
        .answerNestedRepeatedNumberQuestion("lambda", "diez", 10)
        .submit();

    CSVRecord record = getParsedRecords(fakeProgram.id).get(0);

    Stream<String> resultHeaders =
        record.getParser().getHeaderNames().stream().filter(h -> !metadataHeaders.contains(h));
    assertThat(resultHeaders)
        .containsExactly(
            "applicant birth date (date)",
            "applicant household members[0] - household member id (id)",
            // note lexicographic sorting
            "applicant household members[10] - household member id (id)",
            "applicant household members[10] - household members jobs[0] - household members days"
                + " worked (number)",
            "applicant household members[10] - household members jobs[10] - household members days"
                + " worked (number)",
            "applicant household members[10] - household members jobs[1] - household members days"
                + " worked (number)",
            "applicant household members[10] - household members jobs[2] - household members days"
                + " worked (number)",
            "applicant household members[10] - household members jobs[3] - household members days"
                + " worked (number)",
            "applicant household members[10] - household members jobs[4] - household members days"
                + " worked (number)",
            "applicant household members[10] - household members jobs[5] - household members days"
                + " worked (number)",
            "applicant household members[10] - household members jobs[6] - household members days"
                + " worked (number)",
            "applicant household members[10] - household members jobs[7] - household members days"
                + " worked (number)",
            "applicant household members[10] - household members jobs[8] - household members days"
                + " worked (number)",
            "applicant household members[10] - household members jobs[9] - household members days"
                + " worked (number)",
            "applicant household members[1] - household member id (id)",
            "applicant household members[1] - household members jobs[0] - household members days"
                + " worked (number)",
            "applicant household members[1] - household members jobs[1] - household members days"
                + " worked (number)",
            "applicant household members[2] - household member id (id)",
            "applicant household members[3] - household member id (id)",
            "applicant household members[4] - household member id (id)",
            "applicant household members[5] - household member id (id)",
            "applicant household members[6] - household member id (id)",
            "applicant household members[7] - household member id (id)",
            "applicant household members[8] - household member id (id)",
            "applicant household members[9] - household member id (id)",
            "applicant ice cream (selection)",
            // Columns within a question are not sorted by path
            "applicant phone (phone_number)",
            "applicant phone (country_code)");
  }

  @Test
  public void getDemographicsCsv_questionColumnsOrderedByTagAndName() throws Exception {
    // Columns in the demographic CSV are built from an empty ApplicantData, so the demographic CSV
    // will never have columns for repeated questions and their order does not need to be tested.
    createFakeQuestions();
    testQuestionBank.phoneApplicantPhone().addTag(QuestionTag.DEMOGRAPHIC_PII);
    testQuestionBank.phoneApplicantPhone().save();
    testQuestionBank.dateApplicantBirthdate().addTag(QuestionTag.DEMOGRAPHIC_PII);
    testQuestionBank.dateApplicantBirthdate().save();
    testQuestionBank.dropdownApplicantIceCream().addTag(QuestionTag.DEMOGRAPHIC);
    testQuestionBank.dropdownApplicantIceCream().save();
    testQuestionBank.radioApplicantFavoriteSeason().addTag(QuestionTag.DEMOGRAPHIC);
    testQuestionBank.radioApplicantFavoriteSeason().save();

    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.phoneApplicantPhone())
            .withQuestion(testQuestionBank.dateApplicantBirthdate())
            .withQuestion(testQuestionBank.dropdownApplicantIceCream())
            .withQuestion(testQuestionBank.radioApplicantFavoriteSeason())
            .build();

    FakeApplicationFiller.newFillerFor(fakeProgram).submit();

    CSVRecord record = getParsedRecordsFromDemographicCsv().get(0);

    Stream<String> resultHeaders =
        record.getParser().getHeaderNames().stream()
            .filter(h -> !demographicMetadataHeaders.contains(h));
    assertThat(resultHeaders)
        .containsExactly(
            // DEMOGRAPHIC questions are sorted first, then by name
            "applicant favorite season (selection)",
            "applicant ice cream (selection)",
            // DEMOGRAPHIC_PII questions are sorted last, then by name
            "applicant birth date (date)",
            // Columns within a question are not sorted by path
            "applicant phone (phone_number)",
            "applicant phone (country_code)");
  }

  @Test
  public void getProgramAllVersionsCsv_recordsExportedInReverseSubmissionOrder() throws Exception {
    ProgramModel fakeProgram = FakeProgramBuilder.newActiveProgram().build();
    Instant firstSubmissionTime = Instant.parse("2022-12-09T10:30:30.00Z");

    // Create three applications, with submission times in reverse of their creation and ID order.
    // This happens when application A is started before application B, but submitted after
    // application B.
    ApplicationModel appA =
        FakeApplicationFiller.newFillerFor(fakeProgram)
            .atSubmitTime(firstSubmissionTime.plusSeconds(10L))
            .submit()
            .getApplication();
    ApplicationModel appB =
        FakeApplicationFiller.newFillerFor(fakeProgram)
            .atSubmitTime(firstSubmissionTime.plusSeconds(5L))
            .submit()
            .getApplication();
    ApplicationModel appC =
        FakeApplicationFiller.newFillerFor(fakeProgram)
            .atSubmitTime(firstSubmissionTime)
            .submit()
            .getApplication();

    ImmutableList<CSVRecord> records = getParsedRecords(fakeProgram.id);

    assertThat(records.get(0).get("Application ID")).isEqualTo(appA.id.toString());
    assertThat(records.get(1).get("Application ID")).isEqualTo(appB.id.toString());
    assertThat(records.get(2).get("Application ID")).isEqualTo(appC.id.toString());
  }

  @Test
  public void getDemographicsCsv_recordsExportedInAscendingIdOrder() throws Exception {
    ProgramModel fakeProgram = FakeProgramBuilder.newActiveProgram().build();
    Instant firstSubmissionTime = Instant.parse("2022-12-09T10:30:30.00Z");

    // Create three applications, with submission times different than their creation and ID order.
    // This happens when application A is started before application B, but submitted after
    // application B.
    ApplicationModel appA =
        FakeApplicationFiller.newFillerFor(fakeProgram)
            .atSubmitTime(firstSubmissionTime.plusSeconds(5L))
            .submit()
            .getApplication();
    ApplicationModel appB =
        FakeApplicationFiller.newFillerFor(fakeProgram)
            .atSubmitTime(firstSubmissionTime.plusSeconds(10L))
            .submit()
            .getApplication();
    ApplicationModel appC =
        FakeApplicationFiller.newFillerFor(fakeProgram)
            .atSubmitTime(firstSubmissionTime)
            .submit()
            .getApplication();

    ImmutableList<CSVRecord> records = getParsedRecordsFromDemographicCsv();

    assertThat(records.get(0).get("Opaque ID")).isEqualTo(fakeHash(appA.getApplicant().id));
    assertThat(records.get(1).get("Opaque ID")).isEqualTo(fakeHash(appB.getApplicant().id));
    assertThat(records.get(2).get("Opaque ID")).isEqualTo(fakeHash(appC.getApplicant().id));
  }

  @Test
  public void getProgramAllVersionsCsv_whenSubmitterIsTi_TiFieldsAreSet() throws Exception {
    ProgramModel fakeProgram = FakeProgramBuilder.newActiveProgram().build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .byTrustedIntermediary("ti@trusted_intermediaries.org", "TIs Inc.")
        .submit();

    CSVRecord record = getParsedRecords(fakeProgram.id).get(0);

    assertThat(record.get("Submitter Type")).isEqualTo("TRUSTED_INTERMEDIARY");
    assertThat(record.get("TI Email")).isEqualTo("ti@trusted_intermediaries.org");
    assertThat(record.get("TI Organization")).isEqualTo("TIs Inc.");
  }

  @Test
  public void getProgramAllVersionsCsv_whenSubmitterIsApplicant_TiFieldsAreNotSet()
      throws Exception {
    ProgramModel fakeProgram = FakeProgramBuilder.newActiveProgram().build();
    FakeApplicationFiller.newFillerFor(fakeProgram).submit();

    CSVRecord record = getParsedRecords(fakeProgram.id).get(0);

    assertThat(record.get("Submitter Type")).isEqualTo("APPLICANT");
    assertThat(record.get("TI Email")).isEmpty();
    assertThat(record.get("TI Organization")).isEmpty();
  }

  @Test
  public void getDemographicsCsv_whenSubmitterIsTi_TiFieldsAreSet() throws Exception {
    ProgramModel fakeProgram = FakeProgramBuilder.newActiveProgram().build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .byTrustedIntermediary("ti@trusted_intermediaries.org", "TIs Inc.")
        .submit();

    CSVRecord record = getParsedRecordsFromDemographicCsv().get(0);

    assertThat(record.get("Submitter Type")).isEqualTo("TRUSTED_INTERMEDIARY");
    assertThat(record.get("TI Email (Opaque)"))
        .isEqualTo(fakeHash("ti@trusted_intermediaries.org"));
    assertThat(record.get("TI Organization")).isEqualTo("TIs Inc.");
  }

  @Test
  public void getDemographicsCsv_whenSubmitterIsApplicant_TiFieldsAreNotSet() throws Exception {
    ProgramModel fakeProgram = FakeProgramBuilder.newActiveProgram().build();
    FakeApplicationFiller.newFillerFor(fakeProgram).submit();

    CSVRecord record = getParsedRecordsFromDemographicCsv().get(0);

    assertThat(record.get("Submitter Type")).isEqualTo("APPLICANT");
    assertThat(record.get("TI Email (Opaque)")).isEmpty();
    assertThat(record.get("TI Organization")).isEmpty();
  }

  // TODO(#9212): There should never be duplicate entries because question paths should be unique,
  // but due to #9212 there sometimes are. They point at the same location in the applicant data so
  // it doesn't matter which one we keep. Remove this test after this is fixed.
  @Test
  public void getProgramAllVersionsCsv_exportDoesNotFailWithDuplicateQuestion() throws Exception {
    var questionOne =
        testQuestionBank.maybeSave(
            new TextQuestionDefinition(
                QuestionDefinitionConfig.builder()
                    .setName("dupe question 1")
                    .setDescription("")
                    .setQuestionText(LocalizedStrings.of())
                    .setQuestionHelpText(LocalizedStrings.empty())
                    .build()),
            LifecycleStage.ACTIVE);
    var questionTwo =
        testQuestionBank.maybeSave(
            new TextQuestionDefinition(
                QuestionDefinitionConfig.builder()
                    .setName("dupe question 2")
                    .setDescription("")
                    .setQuestionText(LocalizedStrings.of())
                    .setQuestionHelpText(LocalizedStrings.empty())
                    .build()),
            LifecycleStage.ACTIVE);

    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(questionOne)
            .withQuestion(questionTwo)
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerTextQuestion(questionOne, "answer one")
        .answerTextQuestion(questionTwo, "answer two")
        .submit();

    CSVRecord record = getParsedRecords(fakeProgram.id).get(0);

    assertThat(record.get("dupe question  (text)")).isEqualTo("answer two");
  }

  @Test
  public void getProgramAllVersionsCsv_whenAddressQuestionIsAnswered_columnsArePopulated()
      throws Exception {
    createFakeQuestions();
    ImmutableList<ServiceAreaInclusion> serviceAreaInclusions =
        ImmutableList.of(
            ServiceAreaInclusion.builder()
                .setServiceAreaId("cityvilleTownship")
                .setState(ServiceAreaState.IN_AREA)
                .setTimeStamp(1709069741L)
                .build(),
            ServiceAreaInclusion.builder()
                .setServiceAreaId("portland")
                .setState(ServiceAreaState.NOT_IN_AREA)
                .setTimeStamp(1709069741L)
                .build());

    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.addressApplicantAddress())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerCorrectedAddressQuestion(
            testQuestionBank.addressApplicantAddress(),
            "12345 E South St",
            "Apt 8i",
            "CityVille Township",
            "OR",
            "97403",
            CorrectedAddressState.CORRECTED.getSerializationFormat(),
            44.0462,
            -123.0236,
            54321L,
            serviceAreaInclusions)
        .submit();

    CSVRecord record = getParsedRecords(fakeProgram.id).get(0);

    assertThat(record.get("applicant address (street)")).isEqualTo("12345 E South St");
    assertThat(record.get("applicant address (line2)")).isEqualTo("Apt 8i");
    assertThat(record.get("applicant address (city)")).isEqualTo("CityVille Township");
    assertThat(record.get("applicant address (state)")).isEqualTo("OR");
    assertThat(record.get("applicant address (zip)")).isEqualTo("97403");
    assertThat(record.get("applicant address (corrected)")).isEqualTo("Corrected");
    assertThat(record.get("applicant address (latitude)")).isEqualTo("44.0462");
    assertThat(record.get("applicant address (longitude)")).isEqualTo("-123.0236");
    assertThat(record.get("applicant address (well_known_id)")).isEqualTo("54321");
    assertThat(record.get("applicant address (service_area)"))
        .isEqualTo("cityvilleTownship_InArea_1709069741,portland_NotInArea_1709069741");

    // Assert exact set and order of question headers
    Stream<String> resultHeaders =
        record.getParser().getHeaderNames().stream().filter(h -> !metadataHeaders.contains(h));
    assertThat(resultHeaders)
        .containsExactly(
            "applicant address (street)",
            "applicant address (line2)",
            "applicant address (city)",
            "applicant address (state)",
            "applicant address (zip)",
            "applicant address (corrected)",
            "applicant address (latitude)",
            "applicant address (longitude)",
            "applicant address (well_known_id)",
            "applicant address (service_area)");
  }

  @Test
  public void getProgramAllVersionsCsv_whenAddressQuestionIsRepeated_columnsArePopulated()
      throws Exception {
    createFakeQuestions();
    ImmutableList<ServiceAreaInclusion> serviceAreaInclusions =
        ImmutableList.of(
            ServiceAreaInclusion.builder()
                .setServiceAreaId("cityvilleTownship")
                .setState(ServiceAreaState.IN_AREA)
                .setTimeStamp(1709069741L)
                .build(),
            ServiceAreaInclusion.builder()
                .setServiceAreaId("portland")
                .setState(ServiceAreaState.NOT_IN_AREA)
                .setTimeStamp(1709069741L)
                .build());

    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withHouseholdMembersEnumeratorQuestion()
            .withHouseholdMembersRepeatedQuestion(
                testQuestionBank.addressRepeatedHouseholdMemberFavoriteAddress())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerEnumeratorQuestion(ImmutableList.of("taylor"))
        .answerCorrectedAddressQuestion(
            testQuestionBank.addressRepeatedHouseholdMemberFavoriteAddress(),
            "taylor",
            "12345 E South St",
            "Apt 8i",
            "CityVille Township",
            "OR",
            "97403",
            CorrectedAddressState.CORRECTED.getSerializationFormat(),
            44.0462,
            -123.0236,
            54321L,
            serviceAreaInclusions)
        .submit();

    CSVRecord record = getParsedRecords(fakeProgram.id).get(0);

    assertThat(
            record.get(
                "applicant household members[0] - household member favorite address (street)"))
        .isEqualTo("12345 E South St");
    assertThat(
            record.get(
                "applicant household members[0] - household member favorite address (line2)"))
        .isEqualTo("Apt 8i");
    assertThat(
            record.get("applicant household members[0] - household member favorite address (city)"))
        .isEqualTo("CityVille Township");
    assertThat(
            record.get(
                "applicant household members[0] - household member favorite address (state)"))
        .isEqualTo("OR");
    assertThat(
            record.get("applicant household members[0] - household member favorite address (zip)"))
        .isEqualTo("97403");
    assertThat(
            record.get(
                "applicant household members[0] - household member favorite address (corrected)"))
        .isEqualTo("Corrected");
    assertThat(
            record.get(
                "applicant household members[0] - household member favorite address (latitude)"))
        .isEqualTo("44.0462");
    assertThat(
            record.get(
                "applicant household members[0] - household member favorite address (longitude)"))
        .isEqualTo("-123.0236");
    assertThat(
            record.get(
                "applicant household members[0] - household member favorite address"
                    + " (well_known_id)"))
        .isEqualTo("54321");
    assertThat(
            record.get(
                "applicant household members[0] - household member favorite address"
                    + " (service_area)"))
        .isEqualTo("cityvilleTownship_InArea_1709069741,portland_NotInArea_1709069741");
  }

  @Test
  public void getProgramAllVersionsCsv_whenAddressQuestionIsNotAnswered_columnsAreEmpty()
      throws Exception {
    createFakeQuestions();
    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.addressApplicantAddress())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram).submit();

    CSVRecord record = getParsedRecords(fakeProgram.id).get(0);

    assertThat(record.get("applicant address (street)")).isEmpty();
    assertThat(record.get("applicant address (line2)")).isEmpty();
    assertThat(record.get("applicant address (city)")).isEmpty();
    assertThat(record.get("applicant address (state)")).isEmpty();
    assertThat(record.get("applicant address (zip)")).isEmpty();
    assertThat(record.get("applicant address (corrected)")).isEmpty();
    assertThat(record.get("applicant address (latitude)")).isEmpty();
    assertThat(record.get("applicant address (longitude)")).isEmpty();
    assertThat(record.get("applicant address (well_known_id)")).isEmpty();
    assertThat(record.get("applicant address (service_area)")).isEmpty();
  }

  @Test
  public void getProgramAllVersionsCsv_whenCheckboxQuestionIsAnswered_columnsArePopulated()
      throws Exception {
    createFakeQuestions();
    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.checkboxApplicantKitchenTools())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerCheckboxQuestion(
            testQuestionBank.checkboxApplicantKitchenTools(),
            ImmutableList.of(
                2L, // "pepper_grinder"
                3L // "garlic_press"
                ))
        .submit();

    CSVRecord record = getParsedRecords(fakeProgram.id).get(0);

    assertThat(record.get("kitchen tools (selections - toaster)")).isEqualTo("NOT_SELECTED");
    assertThat(record.get("kitchen tools (selections - pepper_grinder)")).isEqualTo("SELECTED");
    assertThat(record.get("kitchen tools (selections - garlic_press)")).isEqualTo("SELECTED");

    // Assert exact set and order of question headers
    Stream<String> resultHeaders =
        record.getParser().getHeaderNames().stream().filter(h -> !metadataHeaders.contains(h));
    assertThat(resultHeaders)
        .containsSequence(
            "kitchen tools (selections - toaster)",
            "kitchen tools (selections - pepper_grinder)",
            "kitchen tools (selections - garlic_press)");
  }

  @Test
  public void getProgramAllVersionsCsv_whenCheckboxQuestionIsRepeated_columnsArePopulated()
      throws Exception {
    createFakeQuestions();
    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withHouseholdMembersEnumeratorQuestion()
            .withHouseholdMembersRepeatedQuestion(
                testQuestionBank.checkboxRepeatedHouseholdMemberUsedAppliances())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerEnumeratorQuestion(ImmutableList.of("taylor"))
        .answerCheckboxQuestion(
            testQuestionBank.checkboxRepeatedHouseholdMemberUsedAppliances(),
            "taylor",
            ImmutableList.of(
                1L, // "dishwasher"
                3L // "washing_machine"
                ))
        .submit();

    CSVRecord record = getParsedRecords(fakeProgram.id).get(0);

    assertThat(
            record.get(
                "applicant household members[0] - household member used appliances (selections -"
                    + " dishwasher)"))
        .isEqualTo("SELECTED");
    assertThat(
            record.get(
                "applicant household members[0] - household member used appliances (selections -"
                    + " stove)"))
        .isEqualTo("NOT_SELECTED");
    assertThat(
            record.get(
                "applicant household members[0] - household member used appliances (selections -"
                    + " washing_machine)"))
        .isEqualTo("SELECTED");
  }

  @Test
  public void
      getProgramAllVersionsCsv_whenCheckboxQuestionIsNotAnswered_columnsArePopulatedWithNotAnswered()
          throws Exception {
    createFakeQuestions();
    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.checkboxApplicantKitchenTools())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram).submit();

    CSVRecord record = getParsedRecords(fakeProgram.id).get(0);

    assertThat(record.get("kitchen tools (selections - toaster)")).isEqualTo("NOT_ANSWERED");
    assertThat(record.get("kitchen tools (selections - pepper_grinder)")).isEqualTo("NOT_ANSWERED");
    assertThat(record.get("kitchen tools (selections - garlic_press)")).isEqualTo("NOT_ANSWERED");
  }

  @Test
  public void
      getProgramAllVersionsCsv_whenCheckboxQuestionHasNewOption_notAnOptionAtProgramVersionIsPopulated()
          throws Exception {
    createFakeQuestions();
    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.checkboxApplicantKitchenTools())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerCheckboxQuestion(
            testQuestionBank.checkboxApplicantKitchenTools(),
            ImmutableList.of(
                2L, // "pepper_grinder"
                3L // "garlic_press"
                ))
        .submit();

    // Update question with new option and publish new version
    QuestionOption newOption =
        QuestionOption.create(4L, 4L, "stand_mixer", LocalizedStrings.of(Locale.US, "stand_mixer"));
    MultiOptionQuestionDefinition questionDefinition =
        (MultiOptionQuestionDefinition)
            testQuestionBank.checkboxApplicantKitchenTools().getQuestionDefinition();
    ImmutableList<QuestionOption> currentOptions = questionDefinition.getOptions();
    ImmutableList<QuestionOption> newOptionList =
        ImmutableList.<QuestionOption>builder().addAll(currentOptions).add(newOption).build();
    QuestionDefinition toUpdate =
        new QuestionDefinitionBuilder(questionDefinition).setQuestionOptions(newOptionList).build();
    questionService.update(toUpdate);
    versionRepository.publishNewSynchronizedVersion();

    CSVRecord record = getParsedRecords(fakeProgram.id).get(0);

    assertThat(record.get("kitchen tools (selections - toaster)")).isEqualTo("NOT_SELECTED");
    assertThat(record.get("kitchen tools (selections - pepper_grinder)")).isEqualTo("SELECTED");
    assertThat(record.get("kitchen tools (selections - garlic_press)")).isEqualTo("SELECTED");
    assertThat(record.get("kitchen tools (selections - stand_mixer)"))
        .isEqualTo("NOT_AN_OPTION_AT_PROGRAM_VERSION");
  }

  @Test
  public void getProgramAllVersionsCsv_whenCurrencyQuestionIsAnswered_columnIsPopulated()
      throws Exception {
    createFakeQuestions();
    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.currencyApplicantMonthlyIncome())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerCurrencyQuestion(testQuestionBank.currencyApplicantMonthlyIncome(), "5,444.33")
        .submit();

    CSVRecord record = getParsedRecords(fakeProgram.id).get(0);

    assertThat(record.get("applicant monthly income (currency)")).isEqualTo("5444.33");

    // Assert exact set and order of question headers
    Stream<String> resultHeaders =
        record.getParser().getHeaderNames().stream().filter(h -> !metadataHeaders.contains(h));
    assertThat(resultHeaders).containsExactly("applicant monthly income (currency)");
  }

  @Test
  public void getProgramAllVersionsCsv_whenCurrencyQuestionIsRepeated_columnIsPopulated()
      throws Exception {
    createFakeQuestions();
    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withHouseholdMembersEnumeratorQuestion()
            .withHouseholdMembersRepeatedQuestion(
                testQuestionBank.currencyRepeatedHouseholdMemberMonthlyIncome())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerEnumeratorQuestion(ImmutableList.of("taylor"))
        .answerCurrencyQuestion(
            testQuestionBank.currencyRepeatedHouseholdMemberMonthlyIncome(), "taylor", "12,345.66")
        .submit();

    CSVRecord record = getParsedRecords(fakeProgram.id).get(0);

    assertThat(
            record.get(
                "applicant household members[0] - household member monthly income (currency)"))
        .isEqualTo("12345.66");
  }

  @Test
  public void getProgramAllVersionsCsv_whenCurrencyQuestionIsNotAnswered_columnIsEmpty()
      throws Exception {
    createFakeQuestions();
    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.currencyApplicantMonthlyIncome())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram).submit();

    CSVRecord record = getParsedRecords(fakeProgram.id).get(0);

    assertThat(record.get("applicant monthly income (currency)")).isEmpty();
  }

  @Test
  public void getProgramAllVersionsCsv_whenDateQuestionIsAnswered_columnIsPopulated()
      throws Exception {
    createFakeQuestions();
    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.dateApplicantBirthdate())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerDateQuestion(testQuestionBank.dateApplicantBirthdate(), "2015-10-21")
        .submit();

    CSVRecord record = getParsedRecords(fakeProgram.id).get(0);

    assertThat(record.get("applicant birth date (date)")).isEqualTo("10/21/2015");
  }

  @Test
  public void getProgramAllVersionsCsv_whenDateQuestionIsNotAnswered_columnIsEmpty()
      throws Exception {
    createFakeQuestions();
    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.dateApplicantBirthdate())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram).submit();

    CSVRecord record = getParsedRecords(fakeProgram.id).get(0);

    assertThat(record.get("applicant birth date (date)")).isEmpty();

    // Assert exact set and order of question headers
    Stream<String> resultHeaders =
        record.getParser().getHeaderNames().stream().filter(h -> !metadataHeaders.contains(h));
    assertThat(resultHeaders).containsExactly("applicant birth date (date)");
  }

  @Test
  public void getProgramAllVersionsCsv_whenDateQuestionIsRepeated_columnIsPopulated()
      throws Exception {
    createFakeQuestions();
    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withHouseholdMembersEnumeratorQuestion()
            .withHouseholdMembersRepeatedQuestion(
                testQuestionBank.dateRepeatedHouseholdMemberBirthdate())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerEnumeratorQuestion(ImmutableList.of("taylor"))
        .answerDateQuestion(
            testQuestionBank.dateRepeatedHouseholdMemberBirthdate(), "taylor", "1989-12-13")
        .submit();

    CSVRecord record = getParsedRecords(fakeProgram.id).get(0);

    assertThat(record.get("applicant household members[0] - household member birth date (date)"))
        .isEqualTo("12/13/1989");
  }

  @Test
  public void getProgramAllVersionsCsv_whenDropdownQuestionIsAnswered_columnIsPopulated()
      throws Exception {
    createFakeQuestions();
    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.dropdownApplicantIceCream())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerDropdownQuestion(testQuestionBank.dropdownApplicantIceCream(), 2L /* strawberry */)
        .submit();

    CSVRecord record = getParsedRecords(fakeProgram.id).get(0);

    assertThat(record.get("applicant ice cream (selection)")).isEqualTo("strawberry");

    // Assert exact set and order of question headers
    Stream<String> resultHeaders =
        record.getParser().getHeaderNames().stream().filter(h -> !metadataHeaders.contains(h));
    assertThat(resultHeaders).containsExactly("applicant ice cream (selection)");
  }

  @Test
  public void getProgramAllVersionsCsv_whenDropdownQuestionIsRepeated_columnIsPopulated()
      throws Exception {
    createFakeQuestions();
    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withHouseholdMembersEnumeratorQuestion()
            .withHouseholdMembersRepeatedQuestion(
                testQuestionBank.dropdownRepeatedHouseholdMemberDessert())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerEnumeratorQuestion(ImmutableList.of("taylor"))
        .answerDropdownQuestion(
            testQuestionBank.dropdownRepeatedHouseholdMemberDessert(), "taylor", 1L /* baklava */)
        .submit();

    CSVRecord record = getParsedRecords(fakeProgram.id).get(0);

    assertThat(
            record.get(
                "applicant household members[0] - household member favorite dessert (selection)"))
        .isEqualTo("baklava");
  }

  @Test
  public void getProgramAllVersionsCsv_whenDropdownQuestionIsNotAnswered_columnIsEmpty()
      throws Exception {
    createFakeQuestions();
    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.dropdownApplicantIceCream())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram).submit();

    CSVRecord record = getParsedRecords(fakeProgram.id).get(0);

    assertThat(record.get("applicant ice cream (selection)")).isEmpty();
  }

  @Test
  public void getProgramAllVersionsCsv_whenEmailQuestionIsAnswered_columnIsPopulated()
      throws Exception {
    createFakeQuestions();
    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.emailApplicantEmail())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerEmailQuestion(testQuestionBank.emailApplicantEmail(), "chell@aperturescience.com")
        .submit();

    CSVRecord record = getParsedRecords(fakeProgram.id).get(0);

    assertThat(record.get("applicant email address (email)"))
        .isEqualTo("chell@aperturescience.com");

    // Assert exact set and order of question headers
    Stream<String> resultHeaders =
        record.getParser().getHeaderNames().stream().filter(h -> !metadataHeaders.contains(h));
    assertThat(resultHeaders).containsExactly("applicant email address (email)");
  }

  @Test
  public void getProgramAllVersionsCsv_whenEmailQuestionIsRepeated_columnIsPopulated()
      throws Exception {
    createFakeQuestions();
    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withHouseholdMembersEnumeratorQuestion()
            .withHouseholdMembersRepeatedQuestion(
                testQuestionBank.emailRepeatedHouseholdMemberEmail())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerEnumeratorQuestion(ImmutableList.of("chell"))
        .answerEmailQuestion(
            testQuestionBank.emailRepeatedHouseholdMemberEmail(),
            "chell",
            "chell@aperturescience.com")
        .submit();

    CSVRecord record = getParsedRecords(fakeProgram.id).get(0);

    assertThat(
            record.get("applicant household members[0] - household member email address (email)"))
        .isEqualTo("chell@aperturescience.com");
  }

  @Test
  public void getProgramAllVersionsCsv_whenEmailQuestionIsNotAnswered_columnIsEmpty()
      throws Exception {
    createFakeQuestions();
    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.emailApplicantEmail())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram).submit();

    CSVRecord record = getParsedRecords(fakeProgram.id).get(0);

    assertThat(record.get("applicant email address (email)")).isEmpty();
  }

  @Test
  public void getProgramAllVersionsCsv_whenFileUploadQuestionIsAnswered_columnIsPopulated()
      throws Exception {
    createFakeQuestions();
    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.fileUploadApplicantFile())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerFileQuestionWithMultipleUpload(
            testQuestionBank.fileUploadApplicantFile(),
            ImmutableList.of("test-file-key-1", "test-file-key-2"))
        .submit();

    CSVRecord record = getParsedRecords(fakeProgram.id).get(0);

    assertThat(record.get("applicant file (file_urls)"))
        .isEqualTo(
            "%s/admin/applicant-files/test-file-key-1, %s/admin/applicant-files/test-file-key-2"
                .formatted(BASE_URL, BASE_URL));

    // Assert exact set and order of question headers
    Stream<String> resultHeaders =
        record.getParser().getHeaderNames().stream().filter(h -> !metadataHeaders.contains(h));
    assertThat(resultHeaders).containsExactly("applicant file (file_urls)");
  }

  @Test
  public void getProgramAllVersionsCsv_whenFileUploadQuestionIsRepeated_columnIsPopulated()
      throws Exception {
    createFakeQuestions();
    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withHouseholdMembersEnumeratorQuestion()
            .withHouseholdMembersRepeatedQuestion(
                testQuestionBank.fileUploadRepeatedHouseholdMemberFile())
            .build();

    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerEnumeratorQuestion(ImmutableList.of("taylor"))
        .answerFileQuestionWithMultipleUpload(
            testQuestionBank.fileUploadRepeatedHouseholdMemberFile(),
            "taylor",
            ImmutableList.of("test-file-key-1", "test-file-key-2"))
        .submit();

    CSVRecord record = getParsedRecords(fakeProgram.id).get(0);

    assertThat(record.get("applicant household members[0] - household member file (file_urls)"))
        .isEqualTo(
            "%s/admin/applicant-files/test-file-key-1, %s/admin/applicant-files/test-file-key-2"
                .formatted(BASE_URL, BASE_URL));
  }

  @Test
  public void getProgramAllVersionsCsv_whenFileUploadQuestionIsNotAnswered_columnIsEmpty()
      throws Exception {
    createFakeQuestions();
    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.fileUploadApplicantFile())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram).submit();

    CSVRecord record = getParsedRecords(fakeProgram.id).get(0);

    assertThat(record.get("applicant file (file_urls)")).isEmpty();
  }

  @Test
  public void getProgramAllVersionsCsv_whenIdQuestionIsAnswered_columnIsPopulated()
      throws Exception {
    createFakeQuestions();
    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.idApplicantId())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerIdQuestion(testQuestionBank.idApplicantId(), "011235813")
        .submit();

    CSVRecord record = getParsedRecords(fakeProgram.id).get(0);

    assertThat(record.get("applicant id (id)")).isEqualTo("011235813");

    // Assert exact set and order of question headers
    Stream<String> resultHeaders =
        record.getParser().getHeaderNames().stream().filter(h -> !metadataHeaders.contains(h));
    assertThat(resultHeaders).containsExactly("applicant id (id)");
  }

  @Test
  public void getProgramAllVersionsCsv_whenIdQuestionIsRepeated_columnIsPopulated()
      throws Exception {
    createFakeQuestions();
    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withHouseholdMembersEnumeratorQuestion()
            .withHouseholdMembersRepeatedQuestion(testQuestionBank.idRepeatedHouseholdMemberId())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerEnumeratorQuestion(ImmutableList.of("taylor"))
        .answerIdQuestion(testQuestionBank.idRepeatedHouseholdMemberId(), "taylor", "011235813")
        .submit();

    CSVRecord record = getParsedRecords(fakeProgram.id).get(0);

    assertThat(record.get("applicant household members[0] - household member id (id)"))
        .isEqualTo("011235813");
  }

  @Test
  public void getProgramAllVersionsCsv_whenIdQuestionIsNotAnswered_columnIsEmpty()
      throws Exception {
    createFakeQuestions();
    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.idApplicantId())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram).submit();

    CSVRecord record = getParsedRecords(fakeProgram.id).get(0);

    assertThat(record.get("applicant id (id)")).isEmpty();
  }

  @Test
  public void getProgramAllVersionsCsv_whenNameQuestionIsAnswered_columnsArePopulated()
      throws Exception {
    createFakeQuestions();
    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.nameApplicantName())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerNameQuestion(testQuestionBank.nameApplicantName(), "Taylor", "Allison", "Swift", "I")
        .submit();

    CSVRecord record = getParsedRecords(fakeProgram.id).get(0);

    assertThat(record.get("applicant name (first_name)")).isEqualTo("Taylor");
    assertThat(record.get("applicant name (middle_name)")).isEqualTo("Allison");
    assertThat(record.get("applicant name (last_name)")).isEqualTo("Swift");
    assertThat(record.get("applicant name (suffix)")).isEqualTo("I");

    // Assert exact set and order of question headers
    Stream<String> resultHeaders =
        record.getParser().getHeaderNames().stream().filter(h -> !metadataHeaders.contains(h));
    assertThat(resultHeaders)
        .containsExactly(
            "applicant name (first_name)",
            "applicant name (middle_name)",
            "applicant name (last_name)",
            "applicant name (suffix)");
  }

  @Test
  public void getProgramAllVersionsCsv_whenNameQuestionIsRepeated_columnsArePopulated()
      throws Exception {
    createFakeQuestions();
    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withHouseholdMembersEnumeratorQuestion()
            .withHouseholdMembersRepeatedQuestion(
                testQuestionBank.nameRepeatedApplicantHouseholdMemberName())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerEnumeratorQuestion(ImmutableList.of("taylor"))
        .answerNameQuestion(
            testQuestionBank.nameRepeatedApplicantHouseholdMemberName(),
            "taylor",
            "Taylor",
            "Allison",
            "Swift",
            "I")
        .submit();

    CSVRecord record = getParsedRecords(fakeProgram.id).get(0);

    assertThat(record.get("applicant household members[0] - household members name (first_name)"))
        .isEqualTo("Taylor");
    assertThat(record.get("applicant household members[0] - household members name (middle_name)"))
        .isEqualTo("Allison");
    assertThat(record.get("applicant household members[0] - household members name (last_name)"))
        .isEqualTo("Swift");
    assertThat(record.get("applicant household members[0] - household members name (suffix)"))
        .isEqualTo("I");
  }

  @Test
  public void getProgramAllVersionsCsv_whenNameQuestionIsNotAnswered_columnsAreEmpty()
      throws Exception {
    createFakeQuestions();
    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.nameApplicantName())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram).submit();

    CSVRecord record = getParsedRecords(fakeProgram.id).get(0);

    assertThat(record.get("applicant name (first_name)")).isEmpty();
    assertThat(record.get("applicant name (middle_name)")).isEmpty();
    assertThat(record.get("applicant name (last_name)")).isEmpty();
    assertThat(record.get("applicant name (suffix)")).isEmpty();
  }

  @Test
  public void getProgramAllVersionsCsv_whenNumberQuestionIsAnswered_columnIsPopulated()
      throws Exception {
    createFakeQuestions();
    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.numberApplicantJugglingNumber())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerNumberQuestion(testQuestionBank.numberApplicantJugglingNumber(), 4200)
        .submit();

    CSVRecord record = getParsedRecords(fakeProgram.id).get(0);

    assertThat(record.get("number of items applicant can juggle (number)")).isEqualTo("4200");

    // Assert exact set and order of question headers
    Stream<String> resultHeaders =
        record.getParser().getHeaderNames().stream().filter(h -> !metadataHeaders.contains(h));
    assertThat(resultHeaders).containsExactly("number of items applicant can juggle (number)");
  }

  @Test
  public void getProgramAllVersionsCsv_whenNumberQuestionIsRepeated_columnIsPopulated()
      throws Exception {
    createFakeQuestions();
    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withHouseholdMembersEnumeratorQuestion()
            .withHouseholdMembersRepeatedQuestion(
                testQuestionBank.numberRepeatedHouseholdMemberNumber())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerEnumeratorQuestion(ImmutableList.of("taylor"))
        .answerNumberQuestion(testQuestionBank.numberRepeatedHouseholdMemberNumber(), "taylor", 13L)
        .submit();

    CSVRecord record = getParsedRecords(fakeProgram.id).get(0);

    assertThat(
            record.get(
                "applicant household members[0] - household member favorite number (number)"))
        .isEqualTo("13");
  }

  @Test
  public void getProgramAllVersionsCsv_whenNumberQuestionIsNotAnswered_columnIsEmpty()
      throws Exception {
    createFakeQuestions();
    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.numberApplicantJugglingNumber())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram).submit();

    CSVRecord record = getParsedRecords(fakeProgram.id).get(0);

    assertThat(record.get("number of items applicant can juggle (number)")).isEmpty();
  }

  @Test
  public void getProgramAllVersionsCsv_whenPhoneQuestionIsAnswered_columnsArePopulated()
      throws Exception {
    createFakeQuestions();
    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.phoneApplicantPhone())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerPhoneQuestion(testQuestionBank.phoneApplicantPhone(), "US", "(555) 867-5309")
        .submit();

    CSVRecord record = getParsedRecords(fakeProgram.id).get(0);

    assertThat(record.get("applicant phone (phone_number)")).isEqualTo("5558675309");
    assertThat(record.get("applicant phone (country_code)")).isEqualTo("US");

    // Assert exact set and order of question headers
    Stream<String> resultHeaders =
        record.getParser().getHeaderNames().stream().filter(h -> !metadataHeaders.contains(h));
    assertThat(resultHeaders)
        .containsExactly("applicant phone (phone_number)", "applicant phone (country_code)");
  }

  @Test
  public void getProgramAllVersionsCsv_whenPhoneQuestionIsRepeated_columnsArePopulated()
      throws Exception {
    createFakeQuestions();
    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withHouseholdMembersEnumeratorQuestion()
            .withHouseholdMembersRepeatedQuestion(
                testQuestionBank.phoneRepeatedHouseholdMemberCell())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerEnumeratorQuestion(ImmutableList.of("taylor"))
        .answerPhoneQuestion(
            testQuestionBank.phoneRepeatedHouseholdMemberCell(), "taylor", "US", "(555) 133-1313")
        .submit();

    CSVRecord record = getParsedRecords(fakeProgram.id).get(0);

    assertThat(record.get("applicant household members[0] - household member cell (phone_number)"))
        .isEqualTo("5551331313");
    assertThat(record.get("applicant household members[0] - household member cell (country_code)"))
        .isEqualTo("US");
  }

  @Test
  public void getProgramAllVersionsCsv_whenPhoneQuestionIsNotAnswered_columnsAreEmpty()
      throws Exception {
    createFakeQuestions();
    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.phoneApplicantPhone())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram).submit();

    CSVRecord record = getParsedRecords(fakeProgram.id).get(0);

    assertThat(record.get("applicant phone (phone_number)")).isEmpty();
    assertThat(record.get("applicant phone (country_code)")).isEmpty();
  }

  @Test
  public void getProgramAllVersionsCsv_whenRadioButtonQuestionIsAnswered_columnIsPopulated()
      throws Exception {
    createFakeQuestions();
    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.radioApplicantFavoriteSeason())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerRadioButtonQuestion(testQuestionBank.radioApplicantFavoriteSeason(), 3L /* summer */)
        .submit();

    CSVRecord record = getParsedRecords(fakeProgram.id).get(0);

    assertThat(record.get("applicant favorite season (selection)")).isEqualTo("summer");

    // Assert exact set and order of question headers
    Stream<String> resultHeaders =
        record.getParser().getHeaderNames().stream().filter(h -> !metadataHeaders.contains(h));
    assertThat(resultHeaders).containsExactly("applicant favorite season (selection)");
  }

  @Test
  public void getProgramAllVersionsCsv_whenRadioButtonQuestionIsRepeated_columnIsPopulated()
      throws Exception {
    createFakeQuestions();
    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withHouseholdMembersEnumeratorQuestion()
            .withHouseholdMembersRepeatedQuestion(
                testQuestionBank.radioRepeatedHouseholdMemberFavoritePrecipitation())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerEnumeratorQuestion(ImmutableList.of("taylor"))
        .answerDropdownQuestion(
            testQuestionBank.radioRepeatedHouseholdMemberFavoritePrecipitation(),
            "taylor",
            2L /* snow */)
        .submit();

    CSVRecord record = getParsedRecords(fakeProgram.id).get(0);

    assertThat(
            record.get(
                "applicant household members[0] - household member favorite precipitation"
                    + " (selection)"))
        .isEqualTo("snow");
  }

  @Test
  public void getProgramAllVersionsCsv_whenRadioButtonQuestionIsNotAnswered_columnIsEmpty()
      throws Exception {
    createFakeQuestions();
    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.radioApplicantFavoriteSeason())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram).submit();

    CSVRecord record = getParsedRecords(fakeProgram.id).get(0);

    assertThat(record.get("applicant favorite season (selection)")).isEmpty();
  }

  @Test
  public void getProgramAllVersionsCsv_whenTextQuestionIsAnswered_columnIsPopulated()
      throws Exception {
    createFakeQuestions();
    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.textApplicantFavoriteColor())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerTextQuestion(testQuestionBank.textApplicantFavoriteColor(), "red 💖")
        .submit();

    CSVRecord record = getParsedRecords(fakeProgram.id).get(0);

    assertThat(record.get("applicant favorite color (text)")).isEqualTo("red 💖");

    // Assert exact set and order of question headers
    Stream<String> resultHeaders =
        record.getParser().getHeaderNames().stream().filter(h -> !metadataHeaders.contains(h));
    assertThat(resultHeaders).containsExactly("applicant favorite color (text)");
  }

  @Test
  public void getProgramAllVersionsCsv_whenTextQuestionIsRepeated_columnIsPopulated()
      throws Exception {
    createFakeQuestions();
    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withHouseholdMembersEnumeratorQuestion()
            .withHouseholdMembersRepeatedQuestion(
                testQuestionBank.textRepeatedApplicantHouseholdMemberFavoriteShape())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerEnumeratorQuestion(ImmutableList.of("taylor"))
        .answerTextQuestion(
            testQuestionBank.textRepeatedApplicantHouseholdMemberFavoriteShape(),
            "taylor",
            "circle")
        .submit();

    CSVRecord record = getParsedRecords(fakeProgram.id).get(0);

    assertThat(
            record.get("applicant household members[0] - household member favorite shape (text)"))
        .isEqualTo("circle");
  }

  @Test
  public void getProgramAllVersionsCsv_whenTextQuestionIsNotAnswered_columnIsEmpty()
      throws Exception {
    createFakeQuestions();
    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.textApplicantFavoriteColor())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram).submit();

    CSVRecord record = getParsedRecords(fakeProgram.id).get(0);

    assertThat(record.get("applicant favorite color (text)")).isEmpty();
  }

  @Test
  public void getDemographicsCsv_onlyDemographicQuestionsAreExported() throws Exception {
    createFakeQuestions();
    testQuestionBank.dateApplicantBirthdate().addTag(QuestionTag.DEMOGRAPHIC);
    testQuestionBank.dateApplicantBirthdate().save();
    testQuestionBank.textApplicantFavoriteColor().addTag(QuestionTag.DEMOGRAPHIC_PII);
    testQuestionBank.textApplicantFavoriteColor().save();

    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.dateApplicantBirthdate())
            .withQuestion(testQuestionBank.textApplicantFavoriteColor())
            .withQuestion(testQuestionBank.idApplicantId())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerDateQuestion(testQuestionBank.dateApplicantBirthdate(), "2015-10-21")
        .answerTextQuestion(testQuestionBank.textApplicantFavoriteColor(), "red")
        .answerIdQuestion(testQuestionBank.idApplicantId(), "011245813")
        .submit();

    CSVRecord record = getParsedRecordsFromDemographicCsv().get(0);

    // Assert column values
    assertThat(record.get("applicant birth date (date)")).isEqualTo("10/21/2015");
    assertThat(record.get("applicant favorite color (text)")).isEqualTo(fakeHash("red"));

    // Assert exact set and order of question headers, and that ID question is not exported.
    Stream<String> resultHeaders =
        record.getParser().getHeaderNames().stream()
            .filter(h -> !demographicMetadataHeaders.contains(h));
    assertThat(resultHeaders)
        .containsExactly("applicant birth date (date)", "applicant favorite color (text)");
  }

  @Test
  public void getDemographicsCsv_whenAddressQuestionIsOpaque_columnsAreObfuscated()
      throws Exception {
    createFakeQuestions();
    testQuestionBank.addressApplicantAddress().addTag(QuestionTag.DEMOGRAPHIC_PII);
    testQuestionBank.addressApplicantAddress().save();
    ImmutableList<ServiceAreaInclusion> serviceAreaInclusions =
        ImmutableList.of(
            ServiceAreaInclusion.builder()
                .setServiceAreaId("cityvilleTownship")
                .setState(ServiceAreaState.IN_AREA)
                .setTimeStamp(1709069741L)
                .build(),
            ServiceAreaInclusion.builder()
                .setServiceAreaId("portland")
                .setState(ServiceAreaState.NOT_IN_AREA)
                .setTimeStamp(1709069741L)
                .build());

    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.addressApplicantAddress())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerCorrectedAddressQuestion(
            testQuestionBank.addressApplicantAddress(),
            "12345 E South St",
            "Apt 8i",
            "CityVille Township",
            "OR",
            "97403",
            CorrectedAddressState.CORRECTED.getSerializationFormat(),
            44.0462,
            -123.0236,
            54321L,
            serviceAreaInclusions)
        .submit();

    CSVRecord record = getParsedRecordsFromDemographicCsv().get(0);

    assertThat(record.get("applicant address (street)")).isEqualTo(fakeHash("12345 E South St"));
    assertThat(record.get("applicant address (line2)")).isEqualTo(fakeHash("Apt 8i"));
    assertThat(record.get("applicant address (city)")).isEqualTo(fakeHash("CityVille Township"));
    assertThat(record.get("applicant address (state)")).isEqualTo(fakeHash("OR"));
    assertThat(record.get("applicant address (zip)")).isEqualTo(fakeHash("97403"));
    assertThat(record.get("applicant address (corrected)")).isEqualTo(fakeHash("Corrected"));
    assertThat(record.get("applicant address (latitude)")).isEqualTo(fakeHash("44.0462"));
    assertThat(record.get("applicant address (longitude)")).isEqualTo(fakeHash("-123.0236"));
    assertThat(record.get("applicant address (well_known_id)")).isEqualTo(fakeHash("54321"));
    assertThat(record.get("applicant address (service_area)"))
        .isEqualTo(fakeHash("cityvilleTownship_InArea_1709069741,portland_NotInArea_1709069741"));
  }

  @Test
  public void getDemographicsCsv_whenCheckboxQuestionIsOpaque_columnsAreObfuscated()
      throws Exception {
    createFakeQuestions();
    testQuestionBank.checkboxApplicantKitchenTools().addTag(QuestionTag.DEMOGRAPHIC_PII);
    testQuestionBank.checkboxApplicantKitchenTools().save();
    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.checkboxApplicantKitchenTools())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerCheckboxQuestion(
            testQuestionBank.checkboxApplicantKitchenTools(),
            ImmutableList.of(
                2L, // "pepper_grinder"
                3L // "garlic_press"
                ))
        .submit();

    CSVRecord record = getParsedRecordsFromDemographicCsv().get(0);

    assertThat(record.get("kitchen tools (selections - toaster)"))
        .isEqualTo(fakeHash("NOT_SELECTED"));
    assertThat(record.get("kitchen tools (selections - pepper_grinder)"))
        .isEqualTo(fakeHash("SELECTED"));
    assertThat(record.get("kitchen tools (selections - garlic_press)"))
        .isEqualTo(fakeHash("SELECTED"));
  }

  @Test
  public void getDemographicsCsv_whenCurrencyQuestionIsOpaque_columnIsObfuscated()
      throws Exception {
    createFakeQuestions();
    testQuestionBank.currencyApplicantMonthlyIncome().addTag(QuestionTag.DEMOGRAPHIC_PII);
    testQuestionBank.currencyApplicantMonthlyIncome().save();

    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.currencyApplicantMonthlyIncome())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerCurrencyQuestion(testQuestionBank.currencyApplicantMonthlyIncome(), "5,444.33")
        .submit();

    CSVRecord record = getParsedRecordsFromDemographicCsv().get(0);

    assertThat(record.get("applicant monthly income (currency)")).isEqualTo(fakeHash("5444.33"));
  }

  @Test
  public void getDemographicsCsv_whenDateQuestionIsOpaque_columnIsObfuscated() throws Exception {
    createFakeQuestions();
    testQuestionBank.dateApplicantBirthdate().addTag(QuestionTag.DEMOGRAPHIC_PII);
    testQuestionBank.dateApplicantBirthdate().save();
    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.dateApplicantBirthdate())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerDateQuestion(testQuestionBank.dateApplicantBirthdate(), "2015-10-21")
        .submit();

    CSVRecord record = getParsedRecordsFromDemographicCsv().get(0);

    assertThat(record.get("applicant birth date (date)")).isEqualTo(fakeHash("10/21/2015"));
  }

  @Test
  public void getDemographicsCsv_whenDropdownQuestionIsOpaque_columnIsObfuscated()
      throws Exception {
    createFakeQuestions();
    testQuestionBank.dropdownApplicantIceCream().addTag(QuestionTag.DEMOGRAPHIC_PII);
    testQuestionBank.dropdownApplicantIceCream().save();

    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.dropdownApplicantIceCream())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerDropdownQuestion(testQuestionBank.dropdownApplicantIceCream(), 2L /* strawberry */)
        .submit();

    CSVRecord record = getParsedRecordsFromDemographicCsv().get(0);

    assertThat(record.get("applicant ice cream (selection)")).isEqualTo(fakeHash("strawberry"));
  }

  @Test
  public void getDemographicsCsv_whenEmailQuestionIsOpaque_columnIsObfuscated() throws Exception {
    createFakeQuestions();
    testQuestionBank.emailApplicantEmail().addTag(QuestionTag.DEMOGRAPHIC_PII);
    testQuestionBank.emailApplicantEmail().save();

    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.emailApplicantEmail())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerEmailQuestion(testQuestionBank.emailApplicantEmail(), "chell@aperturescience.com")
        .submit();

    CSVRecord record = getParsedRecordsFromDemographicCsv().get(0);

    assertThat(record.get("applicant email address (email)"))
        .isEqualTo(fakeHash("chell@aperturescience.com"));
  }

  @Test
  public void getDemographicsCsv_whenFileUploadQuestionIsOpaque_columnsAreObfuscated()
      throws Exception {
    createFakeQuestions();
    testQuestionBank.fileUploadApplicantFile().addTag(QuestionTag.DEMOGRAPHIC_PII);
    testQuestionBank.fileUploadApplicantFile().save();

    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.fileUploadApplicantFile())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerFileQuestionWithMultipleUpload(
            testQuestionBank.fileUploadApplicantFile(), ImmutableList.of("test-file-key"))
        .submit();

    CSVRecord record = getParsedRecordsFromDemographicCsv().get(0);

    assertThat(record.get("applicant file (file_urls)"))
        .isEqualTo(fakeHash("%s/admin/applicant-files/test-file-key".formatted(BASE_URL)));
  }

  @Test
  public void getDemographicsCsv_whenIdQuestionIsOpaque_columnIsObfuscated() throws Exception {
    createFakeQuestions();
    testQuestionBank.idApplicantId().addTag(QuestionTag.DEMOGRAPHIC_PII);
    testQuestionBank.idApplicantId().save();

    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.idApplicantId())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerIdQuestion(testQuestionBank.idApplicantId(), "011235813")
        .submit();

    CSVRecord record = getParsedRecordsFromDemographicCsv().get(0);

    assertThat(record.get("applicant id (id)")).isEqualTo(fakeHash("011235813"));
  }

  @Test
  public void getDemographicsCsv_whenNameQuestionIsOpaque_columnsAreObfuscated() throws Exception {
    createFakeQuestions();
    testQuestionBank.nameApplicantName().addTag(QuestionTag.DEMOGRAPHIC_PII);
    testQuestionBank.nameApplicantName().save();

    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.nameApplicantName())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerNameQuestion(testQuestionBank.nameApplicantName(), "Taylor", "Allison", "Swift", "I")
        .submit();

    CSVRecord record = getParsedRecordsFromDemographicCsv().get(0);

    assertThat(record.get("applicant name (first_name)")).isEqualTo(fakeHash("Taylor"));
    assertThat(record.get("applicant name (middle_name)")).isEqualTo(fakeHash("Allison"));
    assertThat(record.get("applicant name (last_name)")).isEqualTo(fakeHash("Swift"));
    assertThat(record.get("applicant name (suffix)")).isEqualTo(fakeHash("I"));
  }

  @Test
  public void getDemographicsCsv_whenNumberQuestionIsOpaque_columnIsObfuscated() throws Exception {
    createFakeQuestions();
    testQuestionBank.numberApplicantJugglingNumber().addTag(QuestionTag.DEMOGRAPHIC_PII);
    testQuestionBank.numberApplicantJugglingNumber().save();

    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.numberApplicantJugglingNumber())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerNumberQuestion(testQuestionBank.numberApplicantJugglingNumber(), 4200)
        .submit();

    CSVRecord record = getParsedRecordsFromDemographicCsv().get(0);

    assertThat(record.get("number of items applicant can juggle (number)"))
        .isEqualTo(fakeHash("4200"));
  }

  @Test
  public void getDemographicsCsv_whenPhoneQuestionIsOpaque_columnsAreObfuscated() throws Exception {
    createFakeQuestions();
    testQuestionBank.phoneApplicantPhone().addTag(QuestionTag.DEMOGRAPHIC_PII);
    testQuestionBank.phoneApplicantPhone().save();

    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.phoneApplicantPhone())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerPhoneQuestion(testQuestionBank.phoneApplicantPhone(), "US", "(555) 867-5309")
        .submit();

    CSVRecord record = getParsedRecordsFromDemographicCsv().get(0);

    assertThat(record.get("applicant phone (phone_number)")).isEqualTo(fakeHash("5558675309"));
    assertThat(record.get("applicant phone (country_code)")).isEqualTo(fakeHash("US"));
  }

  @Test
  public void getDemographicsCsv_whenRadioButtonQuestionIsOpaque_columnIsObfuscated()
      throws Exception {
    createFakeQuestions();
    testQuestionBank.radioApplicantFavoriteSeason().addTag(QuestionTag.DEMOGRAPHIC_PII);
    testQuestionBank.radioApplicantFavoriteSeason().save();

    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.radioApplicantFavoriteSeason())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerRadioButtonQuestion(testQuestionBank.radioApplicantFavoriteSeason(), 3L /* summer */)
        .submit();

    CSVRecord record = getParsedRecordsFromDemographicCsv().get(0);

    assertThat(record.get("applicant favorite season (selection)")).isEqualTo(fakeHash("summer"));
  }

  @Test
  public void getDemographicsCsv_whenTextQuestionIsOpaque_columnIsObfuscated() throws Exception {
    createFakeQuestions();
    testQuestionBank.textApplicantFavoriteColor().addTag(QuestionTag.DEMOGRAPHIC_PII);
    testQuestionBank.textApplicantFavoriteColor().save();

    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.textApplicantFavoriteColor())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerTextQuestion(testQuestionBank.textApplicantFavoriteColor(), "red 💖")
        .submit();

    CSVRecord record = getParsedRecordsFromDemographicCsv().get(0);

    assertThat(record.get("applicant favorite color (text)")).isEqualTo(fakeHash("red 💖"));
  }

  private String fakeHash(String toHash) {
    return Hashing.sha256()
        .newHasher()
        .putString(SECRET_SALT, StandardCharsets.UTF_8)
        .putString(toHash, StandardCharsets.UTF_8)
        .hash()
        .toString();
  }

  private String fakeHash(Long toHash) {
    return Hashing.sha256()
        .newHasher()
        .putString(SECRET_SALT, StandardCharsets.UTF_8)
        .putLong(toHash)
        .hash()
        .toString();
  }

  private ImmutableList<CSVRecord> getParsedRecords(long programId) throws Exception {
    CSVParser parser =
        CSVParser.parse(
            exporterService.getProgramAllVersionsCsv(programId, SubmittedApplicationFilter.EMPTY),
            DEFAULT_FORMAT);
    return ImmutableList.copyOf(parser.getRecords());
  }

  private ImmutableList<CSVRecord> getParsedRecordsFromDemographicCsv() throws Exception {
    CSVParser parser =
        CSVParser.parse(exporterService.getDemographicsCsv(TimeFilter.EMPTY), DEFAULT_FORMAT);
    return ImmutableList.copyOf(parser.getRecords());
  }
}
