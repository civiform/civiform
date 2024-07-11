package services.export;

import static org.assertj.core.api.Assertions.assertThat;
import static play.api.test.Helpers.testServerPort;

import com.google.common.collect.ImmutableList;
import java.util.Locale;
import models.ApplicantModel;
import models.ApplicationModel;
import models.ProgramModel;
import org.junit.Test;
import play.test.Helpers;
import repository.ProgramRepository;
import repository.SubmittedApplicationFilter;
import repository.VersionRepository;
import services.CfJsonDocumentContext;
import services.IdentifierBasedPaginationSpec;
import services.LocalizedStrings;
import services.Path;
import services.geo.CorrectedAddressState;
import services.geo.ServiceAreaInclusion;
import services.geo.ServiceAreaInclusionGroup;
import services.geo.ServiceAreaState;
import services.program.IllegalPredicateOrderingException;
import services.program.ProgramDefinition;
import services.program.ProgramNeedsABlockException;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import services.question.QuestionOption;
import services.question.QuestionService;
import services.question.exceptions.InvalidUpdateException;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;

public class JsonExporterServiceTest extends AbstractExporterTest {

  private static final String BASE_URL = String.format("http://localhost:%d", testServerPort());

  // TODO(#5257): Refactor testAllQuestionTypesWithoutEnumerators() and
  // testQuestionTypesWithEnumerators()
  // into behavior-specific tests. Remaining work is:
  // - Test enumerator questions and other remaining question types.
  // - Test repeated entities where some and none of the repeated questions are answered.
  // - Test that only ACTIVE applications are included in the response

  @Test
  public void testAllQuestionTypesWithoutEnumerators() throws Exception {
    createFakeQuestions();
    createFakeProgram();
    createFakeApplications();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertLengthOf(3);
    testApplicationTopLevelAnswers(fakeProgram, resultAsserter, applicationOne, 2);
    resultAsserter.assertValueAtPath("$[2].status", STATUS_VALUE);
    resultAsserter.assertValueAtApplicationPath(2, ".applicant_name.first_name", "Alice");
    resultAsserter.assertNullValueAtApplicationPath(2, ".applicant_name.middle_name");
    resultAsserter.assertValueAtApplicationPath(2, ".applicant_name.last_name", "Appleton");
    resultAsserter.assertValueAtApplicationPath(2, ".applicant_birth_date.date", "1980-01-01");
    resultAsserter.assertValueAtApplicationPath(
        2, ".applicant_email_address.email", "one@example.com");
    resultAsserter.assertValueAtApplicationPath(2, ".applicant_address.zip", "54321");
    resultAsserter.assertValueAtApplicationPath(2, ".applicant_address.city", "city");
    resultAsserter.assertValueAtApplicationPath(2, ".applicant_address.street", "street st");
    resultAsserter.assertValueAtApplicationPath(2, ".applicant_address.state", "AB");
    resultAsserter.assertValueAtApplicationPath(2, ".applicant_address.line2", "apt 100");
    resultAsserter.assertValueAtApplicationPath(1, ".applicant_phone.phone_number", "+16157571010");
    resultAsserter.assertValueAtApplicationPath(
        2, ".applicant_favorite_color.text", "Some Value \" containing ,,, special characters");
    resultAsserter.assertValueAtApplicationPath(
        2, ".applicant_monthly_income.currency_dollars", 1234.56);
    resultAsserter.assertValueAtApplicationPath(
        2, ".applicant_file.file_key", "http://localhost:9000/admin/applicant-files/my-file-key");
    resultAsserter.assertValueAtApplicationPath(
        2, ".number_of_items_applicant_can_juggle.number", 123456);
    resultAsserter.assertValueAtApplicationPath(2, ".kitchen_tools.selections[0]", "toaster");
    resultAsserter.assertValueAtApplicationPath(
        2, ".kitchen_tools.selections[1]", "pepper_grinder");
    resultAsserter.assertValueAtApplicationPath(2, ".applicant_ice_cream.selection", "strawberry");
    resultAsserter.assertValueAtApplicationPath(
        2, ".applicant_favorite_season.selection", "winter");

    testApplicationTopLevelAnswers(fakeProgram, resultAsserter, applicationTwo, 1);
    resultAsserter.assertValueAtPath("$[1].status", STATUS_VALUE);
    resultAsserter.assertValueAtApplicationPath(1, ".applicant_name.first_name", "Alice");
    resultAsserter.assertNullValueAtApplicationPath(1, ".applicant_name.middle_name");
    resultAsserter.assertValueAtApplicationPath(1, ".applicant_name.last_name", "Appleton");
    resultAsserter.assertValueAtApplicationPath(1, ".applicant_birth_date.date", "1980-01-01");
    resultAsserter.assertValueAtApplicationPath(
        1, ".applicant_email_address.email", "one@example.com");
    resultAsserter.assertValueAtApplicationPath(1, ".applicant_address.zip", "54321");
    resultAsserter.assertValueAtApplicationPath(1, ".applicant_address.city", "city");
    resultAsserter.assertValueAtApplicationPath(1, ".applicant_address.street", "street st");
    resultAsserter.assertValueAtApplicationPath(1, ".applicant_address.state", "AB");
    resultAsserter.assertValueAtApplicationPath(1, ".applicant_address.line2", "apt 100");
    resultAsserter.assertValueAtApplicationPath(
        1, ".applicant_favorite_color.text", "Some Value \" containing ,,, special characters");
    resultAsserter.assertValueAtApplicationPath(
        1, ".applicant_monthly_income.currency_dollars", 1234.56);
    resultAsserter.assertValueAtApplicationPath(
        1, ".applicant_file.file_key", "http://localhost:9000/admin/applicant-files/my-file-key");
    resultAsserter.assertValueAtApplicationPath(
        1, ".number_of_items_applicant_can_juggle.number", 123456);
    resultAsserter.assertValueAtApplicationPath(1, ".kitchen_tools.selections[0]", "toaster");
    resultAsserter.assertValueAtApplicationPath(
        1, ".kitchen_tools.selections[1]", "pepper_grinder");
    resultAsserter.assertValueAtApplicationPath(1, ".applicant_ice_cream.selection", "strawberry");
    resultAsserter.assertValueAtApplicationPath(
        1, ".applicant_favorite_season.selection", "winter");

    testApplicationTopLevelAnswers(fakeProgram, resultAsserter, applicationFour, 0);
    resultAsserter.assertNullValueAtPath("$[0].status");
    resultAsserter.assertValueAtApplicationPath(0, ".applicant_name.first_name", "Bob");
    resultAsserter.assertNullValueAtApplicationPath(0, ".applicant_name.middle_name");
    resultAsserter.assertValueAtApplicationPath(0, ".applicant_name.last_name", "Baker");
  }

  @Test
  public void testCreateAndSubmitTime_exported() throws Exception {
    createFakeQuestions();
    createFakeProgram();
    createFakeApplications();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertValueAtPath("$[0].create_time", "2022-04-09T03:07:02-07:00");
    resultAsserter.assertValueAtPath("$[0].submit_time", "2022-12-09T02:30:30-08:00");
  }

  @Test
  public void testQuestionTypesWithEnumerators() throws Exception {
    createFakeProgramWithEnumeratorAndAnswerQuestions();
    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgramWithEnumerator.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);

    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertLengthOf(3);

    testApplicationTopLevelAnswers(fakeProgramWithEnumerator, resultAsserter, applicationOne, 2);
    testApplicationTopLevelAnswers(fakeProgramWithEnumerator, resultAsserter, applicationTwo, 1);
    testApplicationTopLevelAnswers(fakeProgramWithEnumerator, resultAsserter, applicationThree, 0);
    resultAsserter.assertValueAtApplicationPath(0, ".applicant_name.first_name", "John");
    resultAsserter.assertNullValueAtApplicationPath(0, ".applicant_name.middle_name");
    resultAsserter.assertValueAtApplicationPath(0, ".applicant_name.last_name", "Doe");
    resultAsserter.assertValueAtApplicationPath(0, ".applicant_favorite_color.text", "brown");
    resultAsserter.assertNullValueAtApplicationPath(
        0, ".applicant_monthly_income.currency_dollars");
    resultAsserter.assertValueAtApplicationPath(
        0, ".applicant_household_members.entities[0].household_members_name.last_name", "Jameson");
    resultAsserter.assertNullValueAtApplicationPath(
        0, ".applicant_household_members.entities[0].household_members_name.middle_name");
    resultAsserter.assertValueAtApplicationPath(
        0, ".applicant_household_members.entities[0].household_members_name.first_name", "James");
    resultAsserter.assertValueAtApplicationPath(
        0,
        ".applicant_household_members.entities[0].household_members_jobs.entities[0].household_members_days_worked.number",
        111);
    resultAsserter.assertValueAtApplicationPath(
        0,
        ".applicant_household_members.entities[0].household_members_jobs.entities[1].household_members_days_worked.number",
        222);
    resultAsserter.assertValueAtApplicationPath(
        0,
        ".applicant_household_members.entities[0].household_members_jobs.entities[2].household_members_days_worked.number",
        333);
  }

  private void testApplicationTopLevelAnswers(
      ProgramModel program,
      ResultAsserter resultAsserter,
      ApplicationModel application,
      int resultIndex) {
    resultAsserter.assertValueAtPath(
        "$[" + resultIndex + "].program_name", program.getProgramDefinition().adminName());
    resultAsserter.assertValueAtPath("$[" + resultIndex + "].program_version_id", program.id);
    resultAsserter.assertValueAtPath(
        "$[" + resultIndex + "].applicant_id", application.getApplicant().id);
    resultAsserter.assertValueAtPath("$[" + resultIndex + "].application_id", application.id);
    resultAsserter.assertValueAtPath("$[" + resultIndex + "].language", "en-US");
  }

  @Test
  public void export_whenSubmitterIsTi_tiTopLevelFieldsAreSet() {
    var fakeProgram = FakeProgramBuilder.newActiveProgram().build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .byTrustedIntermediary("ti@trusted_intermediaries.org", "TIs Inc.")
        .submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertValueAtPath("$[0].submitter_type", "TRUSTED_INTERMEDIARY");
    resultAsserter.assertValueAtPath("$[0].ti_email", "ti@trusted_intermediaries.org");
    resultAsserter.assertValueAtPath("$[0].ti_organization", "TIs Inc.");
  }

  @Test
  public void export_whenSubmitterIsApplicant_tiTopLevelFieldsAreNotSet() {
    var fakeProgram = FakeProgramBuilder.newActiveProgram().build();
    FakeApplicationFiller.newFillerFor(fakeProgram).submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertValueAtPath("$[0].submitter_type", "APPLICANT");
    resultAsserter.assertNullValueAtPath("$[0].ti_email");
    resultAsserter.assertNullValueAtPath("$[0].ti_organization");
  }

  @Test
  public void export_whenAddressQuestionIsAnswered_valueIsInResponse() {
    createFakeQuestions();
    var serviceAreas =
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

    var fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.applicantAddress())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerCorrectedAddressQuestion(
            "12345 E South St",
            "Apt 8i",
            "CityVille Township",
            "OR",
            "97403",
            CorrectedAddressState.CORRECTED.getSerializationFormat(),
            44.0462,
            -123.0236,
            54321L,
            ServiceAreaInclusionGroup.serialize(serviceAreas))
        .submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertJsonAtApplicationPath(
        ".applicant_address",
        """
        {
          "city" : "CityVille Township",
          "corrected" : "Corrected",
          "latitude" : "44.0462",
          "line2" : "Apt 8i",
          "longitude" : "-123.0236",
          "question_type" : "ADDRESS",
          "service_area" : "cityvilleTownship_InArea_1709069741,portland_NotInArea_1709069741",
          "state" : "OR",
          "street" : "12345 E South St",
          "well_known_id" : "54321",
          "zip" : "97403"
        }""");
  }

  @Test
  public void export_whenAddressQuestionOnlyRequiredFieldsAreAnswered_unansweredFieldsAreNull() {
    createFakeQuestions();
    var fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.applicantAddress())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerAddressQuestion("12345 E South St", "", "CityVille Township", "OR", "97403")
        .submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertJsonAtApplicationPath(
        ".applicant_address",
        """
        {
          "city" : "CityVille Township",
          "corrected" : null,
          "latitude" : null,
          "line2" : null,
          "longitude" : null,
          "question_type" : "ADDRESS",
          "service_area" : null,
          "state" : "OR",
          "street" : "12345 E South St",
          "well_known_id" : null,
          "zip" : "97403"
        }""");
  }

  @Test
  public void export_whenAddressQuestionIsNotAnswered_valuesInResponseAreNull() {
    createFakeQuestions();
    var fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.applicantAddress())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram).submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertJsonAtApplicationPath(
        ".applicant_address",
        """
        {
          "city" : null,
          "corrected" : null,
          "latitude" : null,
          "line2" : null,
          "longitude" : null,
          "question_type" : "ADDRESS",
          "service_area" : null,
          "state" : null,
          "street" : null,
          "well_known_id" : null,
          "zip" : null
        }""");
  }

  @Test
  public void export_whenCheckboxQuestionIsAnswered_valueIsInResponse() {
    createFakeQuestions();
    var fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.applicantKitchenTools())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerCheckboxQuestion(
            ImmutableList.of(
                2L, // "pepper_grinder"
                3L // "garlic_press"
                ))
        .submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertJsonAtApplicationPath(
        ".kitchen_tools",
        """
        {
          "question_type" : "MULTI_SELECT",
          "selections" : [ "pepper_grinder", "garlic_press" ]
        }""");
  }

  @Test
  public void export_whenCheckboxQuestionIsNotAnswered_valueInResponseIsEmptyArray() {
    createFakeQuestions();
    var fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.applicantKitchenTools())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram).submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertJsonAtApplicationPath(
        ".kitchen_tools",
        """
        {
          "question_type" : "MULTI_SELECT",
          "selections" : [ ]
        }""");
  }

  @Test
  public void export_whenCurrencyQuestionIsAnswered_valueIsInResponse() {
    createFakeQuestions();
    var fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.applicantMonthlyIncome())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram).answerCurrencyQuestion("5,444.33").submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertJsonAtApplicationPath(
        ".applicant_monthly_income",
        """
        {
          "currency_dollars" : 5444.33,
          "question_type" : "CURRENCY"
        }""");
  }

  @Test
  public void export_whenCurrencyQuestionIsNotAnswered_valueInResponseIsNull() {
    createFakeQuestions();
    var fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.applicantMonthlyIncome())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram).submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertJsonAtApplicationPath(
        ".applicant_monthly_income",
        """
        {
          "currency_dollars" : null,
          "question_type" : "CURRENCY"
        }""");
  }

  @Test
  public void export_whenDateQuestionIsAnswered_valueIsInResponse() {
    createFakeQuestions();
    var fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.applicantDate())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram).answerDateQuestion("2015-10-21").submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertJsonAtApplicationPath(
        ".applicant_birth_date",
        """
        {
          "date" : "2015-10-21",
          "question_type" : "DATE"
        }""");
  }

  @Test
  public void export_whenDateQuestionIsNotAnswered_valueInResponseIsNull() {
    createFakeQuestions();
    var fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.applicantDate())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram).submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertJsonAtApplicationPath(
        ".applicant_birth_date",
        """
        {
          "date" : null,
          "question_type" : "DATE"
        }""");
  }

  @Test
  public void export_whenDropdownQuestionIsAnswered_valueIsInResponse() {
    createFakeQuestions();
    var fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.applicantIceCream())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerDropdownQuestion(2L /* strawberry */)
        .submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertJsonAtApplicationPath(
        ".applicant_ice_cream",
        """
        {
          "question_type" : "SINGLE_SELECT",
          "selection" : "strawberry"
        }""");
  }

  @Test
  public void export_whenDropdownQuestionIsNotAnswered_valueInResponseIsNull() {
    createFakeQuestions();
    var fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.applicantIceCream())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram).submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertJsonAtApplicationPath(
        ".applicant_ice_cream",
        """
        {
          "question_type" : "SINGLE_SELECT",
          "selection" : null
        }""");
  }

  @Test
  public void export_whenEmailQuestionIsAnswered_valueIsInResponse() {
    createFakeQuestions();
    var fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.applicantEmail())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerEmailQuestion("chell@aperturescience.com")
        .submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertJsonAtApplicationPath(
        ".applicant_email_address",
        """
        {
          "email" : "chell@aperturescience.com",
          "question_type" : "EMAIL"
        }""");
  }

  @Test
  public void export_whenEmailQuestionIsNotAnswered_valueInResponseIsNull() {
    createFakeQuestions();
    var fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.applicantEmail())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram).submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertJsonAtApplicationPath(
        ".applicant_email_address",
        """
        {
          "email" : null,
          "question_type" : "EMAIL"
        }""");
  }

  @Test
  public void export_whenFileUploadQuestionIsAnswered_valueIsInResponse() {
    createFakeQuestions();
    var fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.applicantFile())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerFileUploadQuestion("test-file-key")
        .submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertJsonAtApplicationPath(
        ".applicant_file",
        """
        {
          "file_key" : "%s/admin/applicant-files/test-file-key",
          "question_type" : "FILE_UPLOAD"
        }"""
            .formatted(BASE_URL));
  }

  @Test
  public void export_whenFileUploadQuestionIsNotAnswered_valueInResponseIsNull() {
    createFakeQuestions();
    var fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.applicantFile())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram).submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertJsonAtApplicationPath(
        ".applicant_file",
        """
        {
          "file_key" : null,
          "question_type" : "FILE_UPLOAD"
        }""");
  }

  @Test
  public void export_whenIdQuestionIsAnswered_valueIsInResponse() {
    createFakeQuestions();
    var fakeProgram =
        FakeProgramBuilder.newActiveProgram().withQuestion(testQuestionBank.applicantId()).build();
    FakeApplicationFiller.newFillerFor(fakeProgram).answerIdQuestion("011235813").submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertJsonAtApplicationPath(
        ".applicant_id",
        """
        {
          "id" : "011235813",
          "question_type" : "ID"
        }""");
  }

  @Test
  public void export_whenIdQuestionIsNotAnswered_valueInResponseIsNull() {
    createFakeQuestions();
    var fakeProgram =
        FakeProgramBuilder.newActiveProgram().withQuestion(testQuestionBank.applicantId()).build();
    FakeApplicationFiller.newFillerFor(fakeProgram).submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertJsonAtApplicationPath(
        ".applicant_id",
        """
        {
          "id" : null,
          "question_type" : "ID"
        }""");
  }

  @Test
  public void export_whenNameQuestionIsAnswered_valueIsInResponse() {
    createFakeQuestions();
    var fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.applicantName())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerNameQuestion("Taylor", "Allison", "Swift")
        .submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertJsonAtApplicationPath(
        ".applicant_name",
        """
        {
          "first_name" : "Taylor",
          "last_name" : "Swift",
          "middle_name" : "Allison",
          "question_type" : "NAME"
        }""");
  }

  @Test
  public void export_whenNameQuestionOnlyRequiredFieldsAreAnswered_unansweredFieldIsNull() {
    createFakeQuestions();
    var fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.applicantName())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerNameQuestion("Taylor", "", "Swift")
        .submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertJsonAtApplicationPath(
        ".applicant_name",
        """
        {
          "first_name" : "Taylor",
          "last_name" : "Swift",
          "middle_name" : null,
          "question_type" : "NAME"
        }""");
  }

  @Test
  public void export_whenNameQuestionIsNotAnswered_valueInResponseIsNull() {
    createFakeQuestions();
    var fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.applicantName())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram).submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertJsonAtApplicationPath(
        ".applicant_name",
        """
        {
          "first_name" : null,
          "last_name" : null,
          "middle_name" : null,
          "question_type" : "NAME"
        }""");
  }

  @Test
  public void export_whenNumberQuestionIsAnswered_valueIsInResponse() {
    createFakeQuestions();
    var fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.applicantJugglingNumber())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram).answerNumberQuestion(42).submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertJsonAtApplicationPath(
        ".number_of_items_applicant_can_juggle",
        """
        {
          "number" : 42,
          "question_type" : "NUMBER"
        }""");
  }

  @Test
  public void export_whenNumberQuestionIsNotAnswered_valueInResponseIsNull() {
    createFakeQuestions();
    var fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.applicantJugglingNumber())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram).submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertJsonAtApplicationPath(
        ".number_of_items_applicant_can_juggle",
        """
        {
          "number" : null,
          "question_type" : "NUMBER"
        }""");
  }

  @Test
  public void export_whenPhoneQuestionIsAnswered_valueIsInResponse() {
    createFakeQuestions();
    var fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.applicantPhone())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerPhoneQuestion("US", "(555) 867-5309")
        .submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertJsonAtApplicationPath(
        ".applicant_phone",
        """
        {
          "phone_number" : "+15558675309",
          "question_type" : "PHONE"
        }""");
  }

  @Test
  public void export_whenPhoneQuestionIsNotAnswered_valueInResponseIsNull() {
    createFakeQuestions();
    var fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.applicantPhone())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram).submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertJsonAtApplicationPath(
        ".applicant_phone",
        """
        {
          "phone_number" : null,
          "question_type" : "PHONE"
        }""");
  }

  @Test
  public void export_whenRadioButtonQuestionIsAnswered_valueIsInResponse() {
    createFakeQuestions();
    var fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.applicantSeason())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerRadioButtonQuestion(3L /* summer */)
        .submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertJsonAtApplicationPath(
        ".applicant_favorite_season",
        """
        {
          "question_type" : "SINGLE_SELECT",
          "selection" : "summer"
        }""");
  }

  @Test
  public void export_whenRadioButtonQuestionIsNotAnswered_valueInResponseIsNull() {
    createFakeQuestions();
    var fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.applicantSeason())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram).submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertJsonAtApplicationPath(
        ".applicant_favorite_season",
        """
        {
          "question_type" : "SINGLE_SELECT",
          "selection" : null
        }""");
  }

  @Test
  public void export_whenTextQuestionIsAnswered_valueIsInResponse() {
    createFakeQuestions();
    var fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.applicantFavoriteColor())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram).answerTextQuestion("circle 💖").submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertJsonAtApplicationPath(
        ".applicant_favorite_color",
        """
        {
          "question_type" : "TEXT",
          "text" : "circle 💖"
        }""");
  }

  @Test
  public void export_whenTextQuestionIsNotAnswered_valueInResponseIsNull() {
    createFakeQuestions();
    var fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.applicantFavoriteColor())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram).submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertJsonAtApplicationPath(
        ".applicant_favorite_color",
        """
        {
          "question_type" : "TEXT",
          "text" : null
        }""");
  }

  @Test
  public void export_whenEnumeratorQuestionIsNotAnswered_valueInResponseIsEmptyArray() {
    createFakeQuestions();
    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram().withHouseholdMembersEnumeratorQuestion().build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerEnumeratorQuestion(ImmutableList.of())
        .submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertJsonAtApplicationPath(
        ".applicant_household_members",
        """
        {
          "entities" : [ ],
          "question_type" : "ENUMERATOR"
        }""");
  }

  @Test
  public void
      export_whenEnumeratorAndRepeatedQuestionsAreAnswered_repeatedQuestionsHaveAnswerInResponse() {
    createFakeQuestions();
    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram().withHouseholdMembersEnumeratorQuestion().build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerEnumeratorQuestion(ImmutableList.of("carly rae", "tswift"))
        .answerRepeatedTextQuestion("tswift", "hearts")
        .answerRepeatedTextQuestion("carly rae", "stars")
        .submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertJsonAtApplicationPath(
        ".applicant_household_members",
        """
        {
          "entities" : [ {
            "entity_name" : "carly rae",
            "household_member_favorite_shape" : {
              "question_type" : "TEXT",
              "text" : "stars"
            }
          }, {
            "entity_name" : "tswift",
            "household_member_favorite_shape" : {
              "question_type" : "TEXT",
              "text" : "hearts"
            }
          } ],
          "question_type" : "ENUMERATOR"
        }""");
  }

  @Test
  public void
      export_whenEnumeratorQuestionIsAnsweredAndRepeatedQuestionIsNot_repeatedQuestionsHaveNullAnswers() {
    createFakeQuestions();
    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram().withHouseholdMembersEnumeratorQuestion().build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerEnumeratorQuestion(ImmutableList.of("carly rae", "tswift"))
        .submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertJsonAtApplicationPath(
        ".applicant_household_members",
        """
        {
          "entities" : [ {
            "entity_name" : "carly rae",
            "household_member_favorite_shape" : {
              "question_type" : "TEXT",
              "text" : null
            }
          }, {
            "entity_name" : "tswift",
            "household_member_favorite_shape" : {
              "question_type" : "TEXT",
              "text" : null
            }
          } ],
          "question_type" : "ENUMERATOR"
        }""");
  }

  @Test
  public void export_whenNestedEnumeratorQuestionsAreNotAnswered_valueInResponseIsEmptyArray() {
    createFakeQuestions();
    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withHouseholdMembersEnumeratorQuestion()
            .withHouseholdMembersJobsNestedEnumeratorQuestion()
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerEnumeratorQuestion(ImmutableList.of("carly rae", "tswift"))
        .submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertJsonAtApplicationPath(
        ".applicant_household_members",
        """
        {
          "entities" : [ {
            "entity_name" : "carly rae",
            "household_member_favorite_shape" : {
              "question_type" : "TEXT",
              "text" : null
            },
            "household_members_jobs" : {
              "entities" : [ ],
              "question_type" : "ENUMERATOR"
            }
          }, {
            "entity_name" : "tswift",
            "household_member_favorite_shape" : {
              "question_type" : "TEXT",
              "text" : null
            },
            "household_members_jobs" : {
              "entities" : [ ],
              "question_type" : "ENUMERATOR"
            }
          } ],
          "question_type" : "ENUMERATOR"
        }""");
  }

  @Test
  public void
      export_whenNestedEnumeratorQuestionsAreAnsweredAndRepeatedQuestionsAreNot_theyAllHaveEntityNames() {
    createFakeQuestions();
    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withHouseholdMembersEnumeratorQuestion()
            .withHouseholdMembersJobsNestedEnumeratorQuestion()
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerEnumeratorQuestion(ImmutableList.of("carly rae", "tswift"))
        .answerNestedEnumeratorQuestion("carly rae", ImmutableList.of("singer", "songwriter"))
        .answerNestedEnumeratorQuestion("tswift", ImmutableList.of("performer", "composer"))
        .submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertJsonAtApplicationPath(
        ".applicant_household_members",
        """
        {
          "entities" : [ {
            "entity_name" : "carly rae",
            "household_member_favorite_shape" : {
              "question_type" : "TEXT",
              "text" : null
            },
            "household_members_jobs" : {
              "entities" : [ {
                "entity_name" : "singer",
                "household_members_days_worked" : {
                  "number" : null,
                  "question_type" : "NUMBER"
                }
              }, {
                "entity_name" : "songwriter",
                "household_members_days_worked" : {
                  "number" : null,
                  "question_type" : "NUMBER"
                }
              } ],
              "question_type" : "ENUMERATOR"
            }
          }, {
            "entity_name" : "tswift",
            "household_member_favorite_shape" : {
              "question_type" : "TEXT",
              "text" : null
            },
            "household_members_jobs" : {
              "entities" : [ {
                "entity_name" : "performer",
                "household_members_days_worked" : {
                  "number" : null,
                  "question_type" : "NUMBER"
                }
              }, {
                "entity_name" : "composer",
                "household_members_days_worked" : {
                  "number" : null,
                  "question_type" : "NUMBER"
                }
              } ],
              "question_type" : "ENUMERATOR"
            }
          } ],
          "question_type" : "ENUMERATOR"
        }""");
  }

  @Test
  public void
      export_whenNestedEnumeratorAndRepeatedQuestionsAreAnswered_theyHaveAnswersInResponse() {
    createFakeQuestions();
    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withHouseholdMembersEnumeratorQuestion()
            .withHouseholdMembersJobsNestedEnumeratorQuestion()
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerEnumeratorQuestion(ImmutableList.of("carly rae", "tswift"))
        .answerNestedEnumeratorQuestion("carly rae", ImmutableList.of("singer", "songwriter"))
        .answerNestedRepeatedNumberQuestion("carly rae", "singer", 34)
        .answerNestedRepeatedNumberQuestion("carly rae", "songwriter", 35)
        .answerNestedEnumeratorQuestion("tswift", ImmutableList.of("performer", "composer"))
        .answerNestedRepeatedNumberQuestion("tswift", "performer", 13)
        .answerNestedRepeatedNumberQuestion("tswift", "composer", 14)
        .submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertJsonAtApplicationPath(
        ".applicant_household_members",
        """
        {
          "entities" : [ {
            "entity_name" : "carly rae",
            "household_member_favorite_shape" : {
              "question_type" : "TEXT",
              "text" : null
            },
            "household_members_jobs" : {
              "entities" : [ {
                "entity_name" : "singer",
                "household_members_days_worked" : {
                  "number" : 34,
                  "question_type" : "NUMBER"
                }
              }, {
                "entity_name" : "songwriter",
                "household_members_days_worked" : {
                  "number" : 35,
                  "question_type" : "NUMBER"
                }
              } ],
              "question_type" : "ENUMERATOR"
            }
          }, {
            "entity_name" : "tswift",
            "household_member_favorite_shape" : {
              "question_type" : "TEXT",
              "text" : null
            },
            "household_members_jobs" : {
              "entities" : [ {
                "entity_name" : "performer",
                "household_members_days_worked" : {
                  "number" : 13,
                  "question_type" : "NUMBER"
                }
              }, {
                "entity_name" : "composer",
                "household_members_days_worked" : {
                  "number" : 14,
                  "question_type" : "NUMBER"
                }
              } ],
              "question_type" : "ENUMERATOR"
            }
          } ],
          "question_type" : "ENUMERATOR"
        }""");
  }

  @Test
  public void export_questionWithVisibilityPredicate_isInResponseWhenHiddenFromApplicant() {
    createFakeQuestions();
    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withDateQuestionWithVisibilityPredicateOnTextQuestion()
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram).answerTextQuestion("red").submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    // assert answered question
    resultAsserter.assertJsonAtApplicationPath(
        ".applicant_favorite_color",
        """
        {
          "question_type" : "TEXT",
          "text" : "red"
        }""");

    // assert hidden question is still in export
    resultAsserter.assertJsonAtApplicationPath(
        ".applicant_birth_date",
        """
        {
          "date" : null,
          "question_type" : "DATE"
        }""");
  }

  @Test
  public void export_whenApplicationIsActive_revisionStateIsCurrent() {
    var fakeProgram = FakeProgramBuilder.newActiveProgram().build();
    FakeApplicationFiller.newFillerFor(fakeProgram).submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertValueAtPath("$[0].revision_state", "CURRENT");
  }

  @Test
  public void export_whenApplicationIsObsolete_revisionStateIsObsolete() {
    var fakeProgram = FakeProgramBuilder.newActiveProgram().build();
    FakeApplicationFiller.newFillerFor(fakeProgram).submit().markObsolete();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertValueAtPath("$[0].revision_state", "OBSOLETE");
  }

  @Test
  public void export_whenQuestionIsAddedToProgram_itIsInResponseForAllApplications()
      throws ProgramNotFoundException {
    createFakeQuestions();
    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.applicantJugglingNumber())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram).answerNumberQuestion(3).submit();

    ProgramModel updatedFakeProgram =
        FakeProgramBuilder.newDraftOf(fakeProgram)
            .withQuestion(testQuestionBank.applicantEmail())
            .build();
    FakeApplicationFiller.newFillerFor(updatedFakeProgram)
        .answerNumberQuestion(4)
        .answerEmailQuestion("test@test.com")
        .submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            updatedFakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    // first application
    resultAsserter.assertJsonAtApplicationPath(
        1,
        ".applicant_email_address",
        """
        {
          "email" : null,
          "question_type" : "EMAIL"
        }""");
    resultAsserter.assertJsonAtApplicationPath(
        1,
        ".number_of_items_applicant_can_juggle",
        """
        {
          "number" : 3,
          "question_type" : "NUMBER"
        }""");

    // second application
    resultAsserter.assertJsonAtApplicationPath(
        0,
        ".applicant_email_address",
        """
        {
          "email" : "test@test.com",
          "question_type" : "EMAIL"
        }""");
    resultAsserter.assertJsonAtApplicationPath(
        0,
        ".number_of_items_applicant_can_juggle",
        """
        {
          "number" : 4,
          "question_type" : "NUMBER"
        }""");
  }

  @Test
  public void export_whenQuestionIsRemovedFromProgram_itIsStillInResponseForAllApplications()
      throws ProgramNotFoundException,
          ProgramNeedsABlockException,
          IllegalPredicateOrderingException {
    var programService = instanceOf(ProgramService.class);
    createFakeQuestions();

    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.applicantJugglingNumber())
            .withQuestion(testQuestionBank.applicantEmail())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerNumberQuestion(3)
        .answerEmailQuestion("test@test.com")
        .submit();

    ProgramModel updatedProgram =
        FakeProgramBuilder.removeBlockWithQuestion(fakeProgram, testQuestionBank.applicantEmail())
            .build();
    FakeApplicationFiller.newFillerFor(updatedProgram).answerNumberQuestion(4).submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);
    String resultJsonString =
        exporter.export(
            updatedProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    // first application
    resultAsserter.assertJsonAtApplicationPath(
        1,
        ".applicant_email_address",
        """
        {
          "email" : "test@test.com",
          "question_type" : "EMAIL"
        }""");
    resultAsserter.assertJsonAtApplicationPath(
        1,
        ".number_of_items_applicant_can_juggle",
        """
        {
          "number" : 3,
          "question_type" : "NUMBER"
        }""");

    // second application
    resultAsserter.assertJsonAtApplicationPath(
        0,
        ".applicant_email_address",
        """
        {
          "email" : null,
          "question_type" : "EMAIL"
        }""");
    resultAsserter.assertJsonAtApplicationPath(
        0,
        ".number_of_items_applicant_can_juggle",
        """
        {
          "number" : 4,
          "question_type" : "NUMBER"
        }""");

    ImmutableList<ProgramDefinition> programDefinitionsForAllVersions =
        programService.getAllVersionsFullProgramDefinition(fakeProgram.id);
    assertThat(programDefinitionsForAllVersions).hasSize(2);
  }

  @Test
  public void
      export_whenQuestionIsAddedToProgram_exportOnlyIncludesAnswersFromOriginalProgramVersion()
          throws ProgramNotFoundException {
    // An exported program should contain questions for all program versions, but
    // it should only include *answers* to questions that were in the program at the time
    // the application was submitted, even if the applicant had answered the question
    // as a part of another program.
    JsonExporterService exporter = instanceOf(JsonExporterService.class);
    ApplicantModel applicant = resourceCreator.insertApplicantWithAccount();
    createFakeQuestions();

    // Create programs A and B
    ProgramModel fakeProgramA =
        FakeProgramBuilder.newActiveProgram("fake program A")
            .withQuestion(testQuestionBank.applicantJugglingNumber())
            .build();
    ProgramModel fakeProgramB =
        FakeProgramBuilder.newActiveProgram("fake program B")
            .withQuestion(testQuestionBank.applicantEmail())
            .build();

    // Fill out both programs
    FakeApplicationFiller.newFillerFor(fakeProgramA, applicant).answerNumberQuestion(3).submit();
    FakeApplicationFiller.newFillerFor(fakeProgramB, applicant)
        .answerEmailQuestion("test@test.com")
        .submit();

    String programBResultJson =
        exporter.export(
            fakeProgramB.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter programBResultAsserter = new ResultAsserter(programBResultJson);

    // Assert application only includes 1 question
    programBResultAsserter.assertJsonAtApplicationPath(
        ".applicant_email_address",
        """
        {
          "email" : "test@test.com",
          "question_type" : "EMAIL"
        }""");
    programBResultAsserter.assertJsonDoesNotContainApplicationPath(
        ".number_of_items_applicant_can_juggle");

    // Add question from program A to program B
    ProgramModel updatedFakeProgramB =
        FakeProgramBuilder.newDraftOf(fakeProgramB)
            .withQuestion(testQuestionBank.applicantJugglingNumber())
            .build();

    // Without updating the application, re-export it
    String updatedProgramBResultJson =
        exporter.export(
            updatedFakeProgramB.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter updatedProgramBResultAsserter = new ResultAsserter(updatedProgramBResultJson);

    // Assert question from program A is not answered, even though there is an
    // answer in the applicant data.
    updatedProgramBResultAsserter.assertJsonAtApplicationPath(
        ".applicant_email_address",
        """
        {
          "email" : "test@test.com",
          "question_type" : "EMAIL"
        }""");
    updatedProgramBResultAsserter.assertJsonAtApplicationPath(
        ".number_of_items_applicant_can_juggle",
        """
        {
          "number" : null,
          "question_type" : "NUMBER"
        }""");
  }

  @Test
  public void export_whenOptionIsAddedToMultiOptionQuestion_newOptionSelectionIsExportedCorrectly()
      throws UnsupportedQuestionTypeException, InvalidUpdateException, ProgramNotFoundException {
    JsonExporterService exporter = instanceOf(JsonExporterService.class);
    QuestionService questionService = instanceOf(QuestionService.class);
    VersionRepository versionRepository = instanceOf(VersionRepository.class);
    ProgramRepository programRepository = instanceOf(ProgramRepository.class);
    createFakeQuestions();

    var fakeProgram =
        FakeProgramBuilder.newActiveProgram("test new options")
            .withQuestion(testQuestionBank.applicantIceCream())
            .build();

    // Add new question option
    QuestionDefinition questionDefinition =
        testQuestionBank.applicantIceCream().getQuestionDefinition();
    ImmutableList<QuestionOption> newOptions =
        ImmutableList.<QuestionOption>builder()
            .addAll(((MultiOptionQuestionDefinition) questionDefinition).getOptions())
            .add(QuestionOption.create(5L, 5L, "mint", LocalizedStrings.of(Locale.US, "mint")))
            .build();

    QuestionDefinition updatedQuestionDefinition =
        new QuestionDefinitionBuilder(questionDefinition).setQuestionOptions(newOptions).build();
    questionService.update(updatedQuestionDefinition);
    versionRepository.publishNewSynchronizedVersion();

    // Fill out application and select new option
    var updatedFakeProgram =
        programRepository
            .getActiveProgramFromSlug(fakeProgram.getProgramDefinition().slug())
            .toCompletableFuture()
            .join();

    FakeApplicationFiller.newFillerFor(updatedFakeProgram)
        .answerDropdownQuestion(5L) // new "mint" option
        .submit();

    String resultJsonString =
        exporter.export(
            updatedFakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertJsonAtApplicationPath(
        ".applicant_ice_cream",
        """
        {
          "question_type" : "SINGLE_SELECT",
          "selection" : "mint"
        }""");
  }

  private static class ResultAsserter {
    public final CfJsonDocumentContext resultJson;

    ResultAsserter(String resultJsonString) {
      this.resultJson = new CfJsonDocumentContext(resultJsonString);
    }

    private void assertLengthOf(int num) {
      assertThat((int) resultJson.getDocumentContext().read("$.length()")).isEqualTo(num);
    }

    private void assertValueAtPath(String path, String value) {
      assertThat(resultJson.readString(Path.create(path)).get()).isEqualTo(value);
    }

    private void assertValueAtPath(String path, Long value) {
      assertThat(resultJson.readLong(Path.create(path)).get()).isEqualTo(value);
    }

    private void assertNullValueAtPath(String path) {
      Path pathToTest = Path.create(path);
      assertThat(resultJson.hasNullValueAtPath(pathToTest)).isTrue();
    }

    private void assertValueAtApplicationPath(String innerPath, String value) {
      assertValueAtApplicationPath(0, innerPath, value);
    }

    private void assertValueAtApplicationPath(int resultNumber, String innerPath, String value) {
      Path path = Path.create("$[" + resultNumber + "].application" + innerPath);
      assertThat(resultJson.readString(path).get()).isEqualTo(value);
    }

    private void assertValueAtApplicationPath(int resultNumber, String innerPath, int value) {
      Path path = Path.create("$[" + resultNumber + "].application" + innerPath);
      assertThat(resultJson.readLong(path).get()).isEqualTo(value);
    }

    private void assertValueAtApplicationPath(int resultNumber, String innerPath, double value) {
      Path path = Path.create("$[" + resultNumber + "].application" + innerPath);
      assertThat(resultJson.readDouble(path).get()).isEqualTo(value);
    }

    private void assertNullValueAtApplicationPath(String innerPath) {
      assertNullValueAtApplicationPath(0, innerPath);
    }

    private void assertNullValueAtApplicationPath(int resultNumber, String innerPath) {
      Path path = Path.create("$[" + resultNumber + "].application" + innerPath);
      assertThat(resultJson.hasNullValueAtPath(path)).isTrue();
    }

    private void assertJsonDoesNotContainApplicationPath(String innerPath) {
      Path path = Path.create("$[0].application" + innerPath);
      assertThat(resultJson.hasPath(path)).isFalse();
    }

    private void assertJsonAtApplicationPath(
        int resultNumber, String innerPath, String prettyJson) {
      Path path = Path.create("$[" + resultNumber + "].application" + innerPath);
      assertThat(resultJson.asPrettyJsonString(path)).isEqualTo(prettyJson);
    }

    private void assertJsonAtApplicationPath(String innerPath, String prettyJson) {
      assertJsonAtApplicationPath(0, innerPath, prettyJson);
    }
  }
}
