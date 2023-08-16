package services.export;

import static org.assertj.core.api.Assertions.assertThat;
import static services.export.JsonPrettifier.asPrettyJsonString;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import models.Application;
import models.Program;
import org.junit.Test;
import repository.SubmittedApplicationFilter;
import services.CfJsonDocumentContext;
import services.IdentifierBasedPaginationSpec;
import services.Path;

public class JsonExporterTest extends AbstractExporterTest {

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

    JsonExporter exporter = instanceOf(JsonExporter.class);

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
        2, ".kitchen_tools.selections[1]", "pepper grinder");
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
        1, ".kitchen_tools.selections[1]", "pepper grinder");
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

    JsonExporter exporter = instanceOf(JsonExporter.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertValueAtPath("$[0].create_time", "2022/04/09 3:07:02 AM PDT");
    resultAsserter.assertValueAtPath("$[0].submit_time", "2022/12/09 2:30:30 AM PST");
  }

  @Test
  public void testQuestionTypesWithEnumerators() throws Exception {
    createFakeProgramWithEnumeratorAndAnswerQuestions();
    JsonExporter exporter = instanceOf(JsonExporter.class);

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
        0, ".applicant_household_members[0].household_members_name.last_name", "Jameson");
    resultAsserter.assertNullValueAtApplicationPath(
        0, ".applicant_household_members[0].household_members_name.middle_name");
    resultAsserter.assertValueAtApplicationPath(
        0, ".applicant_household_members[0].household_members_name.first_name", "James");
    resultAsserter.assertValueAtApplicationPath(
        0,
        ".applicant_household_members[0].household_members_jobs[0].household_members_days_worked.number",
        111);
    resultAsserter.assertValueAtApplicationPath(
        0,
        ".applicant_household_members[0].household_members_jobs[1].household_members_days_worked.number",
        222);
    resultAsserter.assertValueAtApplicationPath(
        0,
        ".applicant_household_members[0].household_members_jobs[2].household_members_days_worked.number",
        333);
  }

  private void testApplicationTopLevelAnswers(
      Program program, ResultAsserter resultAsserter, Application application, int resultIndex) {
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
    var fakeProgram = new FakeProgramBuilder().build();
    new FakeApplicationFiller(fakeProgram)
        .byTrustedIntermediary("ti@trusted_intermediaries.org", "TIs Inc.")
        .submit();

    JsonExporter exporter = instanceOf(JsonExporter.class);

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
    var fakeProgram = new FakeProgramBuilder().build();
    new FakeApplicationFiller(fakeProgram).submit();

    JsonExporter exporter = instanceOf(JsonExporter.class);

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
    var fakeProgram =
        new FakeProgramBuilder().withQuestion(testQuestionBank.applicantAddress()).build();
    new FakeApplicationFiller(fakeProgram)
        .answerAddressQuestion("12345 E South St", "Apt 8i", "CityVille Township", "OR", "54321")
        .submit();

    JsonExporter exporter = instanceOf(JsonExporter.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertValueAtApplicationPath(".applicant_address.street", "12345 E South St");
    resultAsserter.assertValueAtApplicationPath(".applicant_address.line2", "Apt 8i");
    resultAsserter.assertValueAtApplicationPath(".applicant_address.city", "CityVille Township");
    resultAsserter.assertValueAtApplicationPath(".applicant_address.state", "OR");
    resultAsserter.assertValueAtApplicationPath(".applicant_address.zip", "54321");
  }

  @Test
  public void export_whenAddressQuestionIsNotAnswered_valueInResponseIsNull() {
    createFakeQuestions();
    var fakeProgram =
        new FakeProgramBuilder().withQuestion(testQuestionBank.applicantAddress()).build();
    new FakeApplicationFiller(fakeProgram).submit();

    JsonExporter exporter = instanceOf(JsonExporter.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertNullValueAtApplicationPath(".applicant_address.street");
    resultAsserter.assertNullValueAtApplicationPath(".applicant_address.line2");
    resultAsserter.assertNullValueAtApplicationPath(".applicant_address.city");
    resultAsserter.assertNullValueAtApplicationPath(".applicant_address.state");
    resultAsserter.assertNullValueAtApplicationPath(".applicant_address.zip");
  }

  @Test
  public void export_whenCheckboxQuestionIsAnswered_valueIsInResponse() {
    createFakeQuestions();
    var fakeProgram =
        new FakeProgramBuilder().withQuestion(testQuestionBank.applicantKitchenTools()).build();
    new FakeApplicationFiller(fakeProgram)
        .answerCheckboxQuestion(
            ImmutableList.of(
                2L, // "pepper grinder"
                3L // "garlic press"
                ))
        .submit();

    JsonExporter exporter = instanceOf(JsonExporter.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertValueAtApplicationPath(
        ".kitchen_tools.selections", ImmutableList.of("pepper grinder", "garlic press"));
  }

  @Test
  public void export_whenCheckboxQuestionIsNotAnswered_valueInResponseIsEmptyArray() {
    createFakeQuestions();
    var fakeProgram =
        new FakeProgramBuilder().withQuestion(testQuestionBank.applicantKitchenTools()).build();
    new FakeApplicationFiller(fakeProgram).submit();

    JsonExporter exporter = instanceOf(JsonExporter.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertValueAtApplicationPath(".kitchen_tools.selections", ImmutableList.of());
  }

  @Test
  public void export_whenCurrencyQuestionIsAnswered_valueIsInResponse() {
    createFakeQuestions();
    var fakeProgram =
        new FakeProgramBuilder().withQuestion(testQuestionBank.applicantMonthlyIncome()).build();
    new FakeApplicationFiller(fakeProgram).answerCurrencyQuestion("5,444.33").submit();

    JsonExporter exporter = instanceOf(JsonExporter.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertValueAtApplicationPath(
        ".applicant_monthly_income.currency_dollars", 5444.33);
  }

  @Test
  public void export_whenCurrencyQuestionIsNotAnswered_valueInResponseIsNull() {
    createFakeQuestions();
    var fakeProgram =
        new FakeProgramBuilder().withQuestion(testQuestionBank.applicantMonthlyIncome()).build();
    new FakeApplicationFiller(fakeProgram).submit();

    JsonExporter exporter = instanceOf(JsonExporter.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertNullValueAtApplicationPath(".applicant_monthly_income.currency_dollars");
  }

  @Test
  public void export_whenDateQuestionIsAnswered_valueIsInResponse() {
    createFakeQuestions();
    var fakeProgram =
        new FakeProgramBuilder().withQuestion(testQuestionBank.applicantDate()).build();
    new FakeApplicationFiller(fakeProgram).answerDateQuestion("2015-10-21").submit();

    JsonExporter exporter = instanceOf(JsonExporter.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertValueAtApplicationPath(".applicant_birth_date.date", "2015-10-21");
  }

  @Test
  public void export_whenDateQuestionIsNotAnswered_valueInResponseIsNull() {
    createFakeQuestions();
    var fakeProgram =
        new FakeProgramBuilder().withQuestion(testQuestionBank.applicantDate()).build();
    new FakeApplicationFiller(fakeProgram).submit();

    JsonExporter exporter = instanceOf(JsonExporter.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertNullValueAtApplicationPath(".applicant_birth_date.date");
  }

  @Test
  public void export_whenEmailQuestionIsAnswered_valueIsInResponse() {
    createFakeQuestions();
    var fakeProgram =
        new FakeProgramBuilder().withQuestion(testQuestionBank.applicantEmail()).build();
    new FakeApplicationFiller(fakeProgram)
        .answerEmailQuestion("chell@aperturescience.com")
        .submit();

    JsonExporter exporter = instanceOf(JsonExporter.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertValueAtApplicationPath(
        ".applicant_email_address.email", "chell@aperturescience.com");
  }

  @Test
  public void export_whenEmailQuestionIsNotAnswered_valueInResponseIsNull() {
    createFakeQuestions();
    var fakeProgram =
        new FakeProgramBuilder().withQuestion(testQuestionBank.applicantEmail()).build();
    new FakeApplicationFiller(fakeProgram).submit();

    JsonExporter exporter = instanceOf(JsonExporter.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertNullValueAtApplicationPath(".applicant_email_address.email");
  }

  @Test
  public void export_whenNumberQuestionIsAnswered_valueIsInResponse() {
    createFakeQuestions();
    var fakeProgram =
        new FakeProgramBuilder().withQuestion(testQuestionBank.applicantJugglingNumber()).build();
    new FakeApplicationFiller(fakeProgram).answerNumberQuestion(42).submit();

    JsonExporter exporter = instanceOf(JsonExporter.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertValueAtApplicationPath(".number_of_items_applicant_can_juggle.number", 42);
  }

  @Test
  public void export_whenNumberQuestionIsNotAnswered_valueInResponseIsNull() {
    createFakeQuestions();
    var fakeProgram =
        new FakeProgramBuilder().withQuestion(testQuestionBank.applicantJugglingNumber()).build();
    new FakeApplicationFiller(fakeProgram).submit();

    JsonExporter exporter = instanceOf(JsonExporter.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertNullValueAtApplicationPath(".number_of_items_applicant_can_juggle.number");
  }

  @Test
  public void export_whenPhoneQuestionIsAnswered_valueIsInResponse() {
    createFakeQuestions();
    var fakeProgram =
        new FakeProgramBuilder().withQuestion(testQuestionBank.applicantPhone()).build();
    new FakeApplicationFiller(fakeProgram).answerPhoneQuestion("US", "(555) 867-5309").submit();

    JsonExporter exporter = instanceOf(JsonExporter.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertValueAtApplicationPath(".applicant_phone.phone_number", "+15558675309");
  }

  @Test
  public void export_whenPhoneQuestionIsNotAnswered_valueInResponseIsNull() {
    createFakeQuestions();
    var fakeProgram =
        new FakeProgramBuilder().withQuestion(testQuestionBank.applicantPhone()).build();
    new FakeApplicationFiller(fakeProgram).submit();

    JsonExporter exporter = instanceOf(JsonExporter.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertNullValueAtApplicationPath(".applicant_phone.phone_number");
  }

  @Test
  public void export_whenEnumeratorQuestionIsNotAnswered_valueInResponseIsEmptyArray() {
    createFakeQuestions();
    Program fakeProgram = new FakeProgramBuilder().withHouseholdMembersEnumeratorQuestion().build();
    new FakeApplicationFiller(fakeProgram).answerEnumeratorQuestion(ImmutableList.of()).submit();

    JsonExporter exporter = instanceOf(JsonExporter.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertJsonAtApplicationPath(".applicant_household_members", "[ ]");
  }

  @Test
  public void
      export_whenEnumeratorAndRepeatedQuestionsAreAnswered_repeatedQuestionsHaveAnswerInResponse() {
    createFakeQuestions();
    Program fakeProgram = new FakeProgramBuilder().withHouseholdMembersEnumeratorQuestion().build();
    new FakeApplicationFiller(fakeProgram)
        .answerEnumeratorQuestion(ImmutableList.of("carly rae", "tswift"))
        .answerRepeatedTextQuestion("tswift", "hearts")
        .answerRepeatedTextQuestion("carly rae", "stars")
        .submit();

    JsonExporter exporter = instanceOf(JsonExporter.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertJsonAtApplicationPath(
        ".applicant_household_members",
        "[ {\n"
            + "  \"entity_name\" : \"carly rae\",\n"
            + "  \"household_member_favorite_shape\" : {\n"
            + "    \"text\" : \"stars\"\n"
            + "  }\n"
            + "}, {\n"
            + "  \"entity_name\" : \"tswift\",\n"
            + "  \"household_member_favorite_shape\" : {\n"
            + "    \"text\" : \"hearts\"\n"
            + "  }\n"
            + "} ]");
  }

  @Test
  public void
      export_whenEnumeratorQuestionIsAnsweredAndRepeatedQuestionIsNot_repeatedQuestionsHaveNullAnswers() {
    createFakeQuestions();
    Program fakeProgram = new FakeProgramBuilder().withHouseholdMembersEnumeratorQuestion().build();
    new FakeApplicationFiller(fakeProgram)
        .answerEnumeratorQuestion(ImmutableList.of("carly rae", "tswift"))
        .submit();

    JsonExporter exporter = instanceOf(JsonExporter.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertJsonAtApplicationPath(
        ".applicant_household_members",
        "[ {\n"
            + "  \"entity_name\" : \"carly rae\",\n"
            + "  \"household_member_favorite_shape\" : {\n"
            + "    \"text\" : null\n"
            + "  }\n"
            + "}, {\n"
            + "  \"entity_name\" : \"tswift\",\n"
            + "  \"household_member_favorite_shape\" : {\n"
            + "    \"text\" : null\n"
            + "  }\n"
            + "} ]");
  }

  @Test
  public void export_whenNestedEnumeratorQuestionsAreNotAnswered_valueInResponseIsEmptyArray() {
    createFakeQuestions();
    Program fakeProgram =
        new FakeProgramBuilder()
            .withHouseholdMembersEnumeratorQuestion()
            .withHouseholdMembersJobsNestedEnumeratorQuestion()
            .build();
    new FakeApplicationFiller(fakeProgram)
        .answerEnumeratorQuestion(ImmutableList.of("carly rae", "tswift"))
        .submit();

    JsonExporter exporter = instanceOf(JsonExporter.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertJsonAtApplicationPath(
        ".applicant_household_members",
        "[ {\n"
            + "  \"entity_name\" : \"carly rae\",\n"
            + "  \"household_member_favorite_shape\" : {\n"
            + "    \"text\" : null\n"
            + "  },\n"
            + "  \"household_members_jobs\" : [ ]\n"
            + "}, {\n"
            + "  \"entity_name\" : \"tswift\",\n"
            + "  \"household_member_favorite_shape\" : {\n"
            + "    \"text\" : null\n"
            + "  },\n"
            + "  \"household_members_jobs\" : [ ]\n"
            + "} ]");
  }

  @Test
  public void
      export_whenNestedEnumeratorQuestionsAreAnsweredAndRepeatedQuestionsAreNot_theyAllHaveEntityNames() {
    createFakeQuestions();
    Program fakeProgram =
        new FakeProgramBuilder()
            .withHouseholdMembersEnumeratorQuestion()
            .withHouseholdMembersJobsNestedEnumeratorQuestion()
            .build();
    new FakeApplicationFiller(fakeProgram)
        .answerEnumeratorQuestion(ImmutableList.of("carly rae", "tswift"))
        .answerNestedEnumeratorQuestion("carly rae", ImmutableList.of("singer", "songwriter"))
        .answerNestedEnumeratorQuestion("tswift", ImmutableList.of("performer", "composer"))
        .submit();

    JsonExporter exporter = instanceOf(JsonExporter.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertJsonAtApplicationPath(
        ".applicant_household_members",
        "[ {\n"
            + "  \"entity_name\" : \"carly rae\",\n"
            + "  \"household_member_favorite_shape\" : {\n"
            + "    \"text\" : null\n"
            + "  },\n"
            + "  \"household_members_jobs\" : [ {\n"
            + "    \"entity_name\" : \"singer\",\n"
            + "    \"household_members_days_worked\" : {\n"
            + "      \"number\" : null\n"
            + "    }\n"
            + "  }, {\n"
            + "    \"entity_name\" : \"songwriter\",\n"
            + "    \"household_members_days_worked\" : {\n"
            + "      \"number\" : null\n"
            + "    }\n"
            + "  } ]\n"
            + "}, {\n"
            + "  \"entity_name\" : \"tswift\",\n"
            + "  \"household_member_favorite_shape\" : {\n"
            + "    \"text\" : null\n"
            + "  },\n"
            + "  \"household_members_jobs\" : [ {\n"
            + "    \"entity_name\" : \"performer\",\n"
            + "    \"household_members_days_worked\" : {\n"
            + "      \"number\" : null\n"
            + "    }\n"
            + "  }, {\n"
            + "    \"entity_name\" : \"composer\",\n"
            + "    \"household_members_days_worked\" : {\n"
            + "      \"number\" : null\n"
            + "    }\n"
            + "  } ]\n"
            + "} ]");
  }

  @Test
  public void
      export_whenNestedEnumeratorAndRepeatedQuestionsAreAnswered_theyHaveAnswersInResponse() {
    createFakeQuestions();
    Program fakeProgram =
        new FakeProgramBuilder()
            .withHouseholdMembersEnumeratorQuestion()
            .withHouseholdMembersJobsNestedEnumeratorQuestion()
            .build();
    new FakeApplicationFiller(fakeProgram)
        .answerEnumeratorQuestion(ImmutableList.of("carly rae", "tswift"))
        .answerNestedEnumeratorQuestion("carly rae", ImmutableList.of("singer", "songwriter"))
        .answerNestedRepeatedNumberQuestion("carly rae", "singer", 34)
        .answerNestedRepeatedNumberQuestion("carly rae", "songwriter", 35)
        .answerNestedEnumeratorQuestion("tswift", ImmutableList.of("performer", "composer"))
        .answerNestedRepeatedNumberQuestion("tswift", "performer", 13)
        .answerNestedRepeatedNumberQuestion("tswift", "composer", 14)
        .submit();

    JsonExporter exporter = instanceOf(JsonExporter.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertJsonAtApplicationPath(
        ".applicant_household_members",
        "[ {\n"
            + "  \"entity_name\" : \"carly rae\",\n"
            + "  \"household_member_favorite_shape\" : {\n"
            + "    \"text\" : null\n"
            + "  },\n"
            + "  \"household_members_jobs\" : [ {\n"
            + "    \"entity_name\" : \"singer\",\n"
            + "    \"household_members_days_worked\" : {\n"
            + "      \"number\" : 34\n"
            + "    }\n"
            + "  }, {\n"
            + "    \"entity_name\" : \"songwriter\",\n"
            + "    \"household_members_days_worked\" : {\n"
            + "      \"number\" : 35\n"
            + "    }\n"
            + "  } ]\n"
            + "}, {\n"
            + "  \"entity_name\" : \"tswift\",\n"
            + "  \"household_member_favorite_shape\" : {\n"
            + "    \"text\" : null\n"
            + "  },\n"
            + "  \"household_members_jobs\" : [ {\n"
            + "    \"entity_name\" : \"performer\",\n"
            + "    \"household_members_days_worked\" : {\n"
            + "      \"number\" : 13\n"
            + "    }\n"
            + "  }, {\n"
            + "    \"entity_name\" : \"composer\",\n"
            + "    \"household_members_days_worked\" : {\n"
            + "      \"number\" : 14\n"
            + "    }\n"
            + "  } ]\n"
            + "} ]");
  }

  @Test
  public void export_whenApplicationIsActive_revisionStateIsCurrent() {
    var fakeProgram = new FakeProgramBuilder().build();
    new FakeApplicationFiller(fakeProgram).submit();

    JsonExporter exporter = instanceOf(JsonExporter.class);

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
    var fakeProgram = new FakeProgramBuilder().build();
    new FakeApplicationFiller(fakeProgram).submit().markObsolete();

    JsonExporter exporter = instanceOf(JsonExporter.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertValueAtPath("$[0].revision_state", "OBSOLETE");
  }

  @Test
  public void wrapPayloadJson_wrapsPayloadCorrectly() {
    String payload = "{\"United States\":{\"New York State\":[\"New York City\", \"Albany\"]}}";

    JsonExporter exporter = instanceOf(JsonExporter.class);

    String result =
        exporter.wrapPayloadJson(payload, /* paginationTokenPayload= */ Optional.empty());

    assertThat(asPrettyJsonString(result))
        .isEqualTo(
            "{\n"
                + "  \"nextPageToken\" : null,\n"
                + "  \"payload\" : {\n"
                + "    \"United States\" : {\n"
                + "      \"New York State\" : [ \"New York City\", \"Albany\" ]\n"
                + "    }\n"
                + "  }\n"
                + "}");
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
      assertThat(resultJson.hasPath(pathToTest)).isTrue();
      assertThat(resultJson.readString(pathToTest)).isEmpty();
    }

    private void assertValueAtApplicationPath(String innerPath, String value) {
      assertValueAtApplicationPath(0, innerPath, value);
    }

    private void assertValueAtApplicationPath(int resultNumber, String innerPath, String value) {
      Path path = Path.create("$[" + resultNumber + "].application" + innerPath);
      assertThat(resultJson.readString(path).get()).isEqualTo(value);
    }

    private void assertValueAtApplicationPath(String innerPath, int value) {
      assertValueAtApplicationPath(0, innerPath, value);
    }

    private void assertValueAtApplicationPath(int resultNumber, String innerPath, int value) {
      Path path = Path.create("$[" + resultNumber + "].application" + innerPath);
      assertThat(resultJson.readLong(path).get()).isEqualTo(value);
    }

    private void assertValueAtApplicationPath(String innerPath, double value) {
      assertValueAtApplicationPath(0, innerPath, value);
    }

    private void assertValueAtApplicationPath(int resultNumber, String innerPath, double value) {
      Path path = Path.create("$[" + resultNumber + "].application" + innerPath);
      assertThat(resultJson.readDouble(path).get()).isEqualTo(value);
    }

    private void assertValueAtApplicationPath(String innerPath, ImmutableList<String> list) {
      assertValueAtApplicationPath(0, innerPath, list);
    }

    private void assertValueAtApplicationPath(
        int resultNumber, String innerPath, ImmutableList<String> list) {
      Path path = Path.create("$[" + resultNumber + "].application" + innerPath);
      assertThat(resultJson.readStringList(path)).hasValue(list);
    }

    private void assertNullValueAtApplicationPath(String innerPath) {
      assertNullValueAtApplicationPath(0, innerPath);
    }

    private void assertNullValueAtApplicationPath(int resultNumber, String innerPath) {
      Path path = Path.create("$[" + resultNumber + "].application" + innerPath);
      assertThat(resultJson.hasNullValueAtPath(path)).isTrue();
    }

    private void assertJsonAtApplicationPath(String innerPath, String prettyJson) {
      Path path = Path.create("$[0].application" + innerPath);
      assertThat(resultJson.asPrettyJsonString(path)).isEqualTo(prettyJson);
    }
  }
}
