package services.export;

import static org.assertj.core.api.Assertions.assertThat;
import static play.api.test.Helpers.testServerPort;

import com.google.common.collect.ImmutableList;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import models.ApplicantModel;
import models.LifecycleStage;
import models.ProgramModel;
import org.junit.Test;
import repository.ProgramRepository;
import repository.SubmittedApplicationFilter;
import repository.VersionRepository;
import services.CfJsonDocumentContext;
import services.LocalizedStrings;
import services.Path;
import services.application.ApplicationEventDetails;
import services.application.ApplicationEventDetails.StatusEvent;
import services.geo.CorrectedAddressState;
import services.geo.ServiceAreaInclusion;
import services.geo.ServiceAreaState;
import services.pagination.SubmitTimeSequentialAccessPaginationSpec;
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
import services.question.types.QuestionDefinitionConfig;
import services.question.types.TextQuestionDefinition;

public class JsonExporterServiceTest extends AbstractExporterTest {

  private static final String BASE_URL = String.format("http://localhost:%d", testServerPort());

  @Test
  public void testApplicationsAreInReverseSubmissionOrder() {
    var fakeProgram = FakeProgramBuilder.newActiveProgram("fake-program").build();
    var firstApplication =
        FakeApplicationFiller.newFillerFor(fakeProgram).submit().getApplication();
    var secondApplication =
        FakeApplicationFiller.newFillerFor(fakeProgram).submit().getApplication();
    var thirdApplication =
        FakeApplicationFiller.newFillerFor(fakeProgram).submit().getApplication();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertValueAtPath(0, "application_id", thirdApplication.id);
    resultAsserter.assertValueAtPath(1, "application_id", secondApplication.id);
    resultAsserter.assertValueAtPath(2, "application_id", firstApplication.id);
  }

  @Test
  public void export_testApplicationTopLevelFields() {
    // Times are expected to be exported in instance local time, so we choose
    // multiple timezones here to verify they're updated.
    var createTime = Instant.parse("2015-10-21T05:28:02-06:00");
    var submitTime = Instant.parse("2015-10-22T08:28:02-08:00");
    var fakeProgram = FakeProgramBuilder.newActiveProgram("fake-program").build();
    var applicant = resourceCreator.insertApplicantWithAccount();
    applicant.getApplicantData().setPreferredLocale(Locale.forLanguageTag("ko"));
    applicant.save();
    var application =
        FakeApplicationFiller.newFillerFor(fakeProgram, applicant)
            .atCreateTime(createTime)
            .atSubmitTime(submitTime)
            .submit()
            .getApplication();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertValueAtPath("applicant_id", applicant.id);
    resultAsserter.assertValueAtPath("application_id", application.id);
    resultAsserter.assertValueAtPath("create_time", "2015-10-21T04:28:02-07:00");
    resultAsserter.assertValueAtPath("language", "ko");
    resultAsserter.assertValueAtPath("program_name", "fake-program");
    resultAsserter.assertValueAtPath("program_version_id", fakeProgram.id);
    resultAsserter.assertValueAtPath("revision_state", "CURRENT");
    resultAsserter.assertNullValueAtPath("status");
    resultAsserter.assertValueAtPath("submit_time", "2015-10-22T09:28:02-07:00");
    resultAsserter.assertValueAtPath("submitter_type", "APPLICANT");
    resultAsserter.assertNullValueAtPath("ti_email");
    resultAsserter.assertNullValueAtPath("ti_organization");
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
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertValueAtPath("submitter_type", "TRUSTED_INTERMEDIARY");
    resultAsserter.assertValueAtPath("ti_email", "ti@trusted_intermediaries.org");
    resultAsserter.assertValueAtPath("ti_organization", "TIs Inc.");
  }

  @Test
  public void export_whenSubmitterIsApplicant_tiTopLevelFieldsAreNotSet() {
    var fakeProgram = FakeProgramBuilder.newActiveProgram().build();
    FakeApplicationFiller.newFillerFor(fakeProgram).submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertValueAtPath("submitter_type", "APPLICANT");
    resultAsserter.assertNullValueAtPath("ti_email");
    resultAsserter.assertNullValueAtPath("ti_organization");
  }

  @Test
  public void export_whenApplicationHasStatus_applicationStatusFieldIsSet() throws Exception {
    var status = "approved";
    var admin = resourceCreator.insertAccount();
    var fakeProgram =
        FakeProgramBuilder.newActiveProgram().withStatuses(ImmutableList.of(status)).build();

    var fakeApplicationFiller0 = FakeApplicationFiller.newFillerFor(fakeProgram).submit();
    var fakeApplication0 = fakeApplicationFiller0.getApplication();
    programAdminApplicationService.setStatus(
        fakeApplication0.id,
        fakeProgram.getProgramDefinition(),
        Optional.empty(),
        StatusEvent.builder().setEmailSent(false).setStatusText(status).build(),
        admin);

    FakeApplicationFiller.newFillerFor(fakeProgram).submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    // results are in reverse order from submission
    resultAsserter.assertNullValueAtPath(0, "status");
    resultAsserter.assertValueAtPath(1, "status", "approved");
  }

  @Test
  public void export_whenApplicationHasNote_applicationNoteFieldIsSet() throws Exception {
    var note = "Needs Document";
    var admin = resourceCreator.insertAccount();
    var fakeProgram =
        FakeProgramBuilder.newActiveProgram().withStatuses(ImmutableList.of("Pending")).build();

    var fakeApplicationFiller0 = FakeApplicationFiller.newFillerFor(fakeProgram).submit();
    var fakeApplication0 = fakeApplicationFiller0.getApplication();
    programAdminApplicationService.setNote(
        fakeApplication0, ApplicationEventDetails.NoteEvent.create(note), admin);

    FakeApplicationFiller.newFillerFor(fakeProgram).submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    // results are in reverse order from submission
    resultAsserter.assertNullValueAtPath(0, "application_note");
    resultAsserter.assertValueAtPath(1, "application_note", note);
  }

  // TODO(#9212): There should never be duplicate entries because question paths should be unique,
  // but due to #9212 there sometimes are. They point at the same location in the applicant data so
  // it doesn't matter which one we keep. Remove this test after this is fixed.
  @Test
  public void export_exportDoesNotFailWithDuplicateQuestion() throws Exception {
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

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertJsonAtApplicationPath(
        ".dupe_question_",
        """
        {
          "question_type" : "TEXT",
          "text" : "answer two"
        }""");
  }

  @Test
  public void export_whenAddressQuestionIsAnswered_valueIsInResponse() {
    createFakeQuestions();
    var serviceAreaInclusions =
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

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
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
  public void export_whenAddressQuestionIsRepeated_answersAreCorrectlyNested() {
    createFakeQuestions();
    var serviceAreaInclusions =
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

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertJsonAtApplicationPath(
        ".applicant_household_members",
        """
        {
          "entities" : [ {
            "entity_name" : "taylor",
            "household_member_favorite_address" : {
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
            }
          } ],
          "question_type" : "ENUMERATOR"
        }""");
  }

  @Test
  public void export_whenAddressQuestionOnlyRequiredFieldsAreAnswered_unansweredFieldsAreNull() {
    createFakeQuestions();
    var fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.addressApplicantAddress())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerAddressQuestion(
            testQuestionBank.addressApplicantAddress(),
            "12345 E South St",
            "",
            "CityVille Township",
            "OR",
            "97403")
        .submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
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
            .withQuestion(testQuestionBank.addressApplicantAddress())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram).submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
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

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
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
  public void export_whenCheckboxQuestionIsRepeated_answersAreCorrectlyNested() {
    createFakeQuestions();
    var fakeProgram =
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

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertJsonAtApplicationPath(
        ".applicant_household_members",
        """
        {
          "entities" : [ {
            "entity_name" : "taylor",
            "household_member_used_appliances" : {
              "question_type" : "MULTI_SELECT",
              "selections" : [ "dishwasher", "washing_machine" ]
            }
          } ],
          "question_type" : "ENUMERATOR"
        }""");
  }

  @Test
  public void export_whenCheckboxQuestionIsNotAnswered_valueInResponseIsEmptyArray() {
    createFakeQuestions();
    var fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.checkboxApplicantKitchenTools())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram).submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
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
            .withQuestion(testQuestionBank.currencyApplicantMonthlyIncome())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerCurrencyQuestion(testQuestionBank.currencyApplicantMonthlyIncome(), "5,444.33")
        .submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
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
  public void export_whenCurrencyQuestionIsRepeated_answersAreCorrectlyNested() {
    createFakeQuestions();
    var fakeProgram =
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

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertJsonAtApplicationPath(
        ".applicant_household_members",
        """
        {
          "entities" : [ {
            "entity_name" : "taylor",
            "household_member_monthly_income" : {
              "currency_dollars" : 12345.66,
              "question_type" : "CURRENCY"
            }
          } ],
          "question_type" : "ENUMERATOR"
        }""");
  }

  @Test
  public void export_whenCurrencyQuestionIsNotAnswered_valueInResponseIsNull() {
    createFakeQuestions();
    var fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.currencyApplicantMonthlyIncome())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram).submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
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
            .withQuestion(testQuestionBank.dateApplicantBirthdate())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerDateQuestion(testQuestionBank.dateApplicantBirthdate(), "2015-10-21")
        .submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
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
  public void export_whenDateQuestionIsRepeated_answersAreCorrectlyNested() {
    createFakeQuestions();
    var fakeProgram =
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

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertJsonAtApplicationPath(
        ".applicant_household_members",
        """
        {
          "entities" : [ {
            "entity_name" : "taylor",
            "household_member_birth_date" : {
              "date" : "1989-12-13",
              "question_type" : "DATE"
            }
          } ],
          "question_type" : "ENUMERATOR"
        }""");
  }

  @Test
  public void export_whenDateQuestionIsNotAnswered_valueInResponseIsNull() {
    createFakeQuestions();
    var fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.dateApplicantBirthdate())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram).submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
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
            .withQuestion(testQuestionBank.dropdownApplicantIceCream())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerDropdownQuestion(testQuestionBank.dropdownApplicantIceCream(), 2L /* strawberry */)
        .submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
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
  public void export_whenDropdownQuestionIsRepeated_answersAreCorrectlyNested() {
    createFakeQuestions();
    var fakeProgram =
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

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertJsonAtApplicationPath(
        ".applicant_household_members",
        """
        {
          "entities" : [ {
            "entity_name" : "taylor",
            "household_member_favorite_dessert" : {
              "question_type" : "SINGLE_SELECT",
              "selection" : "baklava"
            }
          } ],
          "question_type" : "ENUMERATOR"
        }""");
  }

  @Test
  public void export_whenDropdownQuestionIsNotAnswered_valueInResponseIsNull() {
    createFakeQuestions();
    var fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.dropdownApplicantIceCream())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram).submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
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
            .withQuestion(testQuestionBank.emailApplicantEmail())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerEmailQuestion(testQuestionBank.emailApplicantEmail(), "chell@aperturescience.com")
        .submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
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
  public void export_whenEmailQuestionIsRepeated_answersAreCorrectlyNested() {
    createFakeQuestions();
    var fakeProgram =
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

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertJsonAtApplicationPath(
        ".applicant_household_members",
        """
        {
          "entities" : [ {
            "entity_name" : "chell",
            "household_member_email_address" : {
              "email" : "chell@aperturescience.com",
              "question_type" : "EMAIL"
            }
          } ],
          "question_type" : "ENUMERATOR"
        }""");
  }

  @Test
  public void export_whenEmailQuestionIsNotAnswered_valueInResponseIsNull() {
    createFakeQuestions();
    var fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.emailApplicantEmail())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram).submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
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
            .withQuestion(testQuestionBank.fileUploadApplicantFile())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerFileQuestionWithMultipleUpload(
            testQuestionBank.fileUploadApplicantFile(),
            ImmutableList.of("test-file-key-1", "test-file-key-2"))
        .submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertJsonAtApplicationPath(
        ".applicant_file",
        """
{
  "file_urls" : [ "%s/admin/applicant-files/test-file-key-1", "%s/admin/applicant-files/test-file-key-2" ],
  "question_type" : "FILE_UPLOAD"
}"""
            .formatted(BASE_URL, BASE_URL));
  }

  @Test
  public void export_whenFileUploadQuestionIsNotAnswered_showsEmptyList() {
    createFakeQuestions();
    var fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.fileUploadApplicantFile())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram).submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertJsonAtApplicationPath(
        ".applicant_file",
        """
        {
          "file_urls" : [ ],
          "question_type" : "FILE_UPLOAD"
        }""");
  }

  @Test
  public void export_whenFileUploadQuestionIsRepeated_answersAreCorrectlyNested() {
    createFakeQuestions();
    var fakeProgram =
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

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertJsonAtApplicationPath(
        ".applicant_household_members",
        """
{
  "entities" : [ {
    "entity_name" : "taylor",
    "household_member_file" : {
      "file_urls" : [ "%s/admin/applicant-files/test-file-key-1", "%s/admin/applicant-files/test-file-key-2" ],
      "question_type" : "FILE_UPLOAD"
    }
  } ],
  "question_type" : "ENUMERATOR"
}"""
            .formatted(BASE_URL, BASE_URL));
  }

  @Test
  public void export_whenFileUploadQuestionIsNotAnswered_showsNullAndEmptyValues() {
    createFakeQuestions();
    var fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.fileUploadApplicantFile())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram).submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertJsonAtApplicationPath(
        ".applicant_file",
        """
        {
          "file_urls" : [ ],
          "question_type" : "FILE_UPLOAD"
        }""");
  }

  @Test
  public void
      export_whenFileUploadQuestionIsRepeated_answersAreCorrectlyNested_andShowBothValues() {
    createFakeQuestions();
    var fakeProgram =
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
            ImmutableList.of("test-file-key"))
        //
        // .answerFileUploadQuestion(testQuestionBank.fileUploadRepeatedHouseholdMemberFile(),
        // "taylor", "test-file-key")
        .submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertJsonAtApplicationPath(
        ".applicant_household_members",
        """
        {
          "entities" : [ {
            "entity_name" : "taylor",
            "household_member_file" : {
              "file_urls" : [ "%s/admin/applicant-files/test-file-key" ],
              "question_type" : "FILE_UPLOAD"
            }
          } ],
          "question_type" : "ENUMERATOR"
        }"""
            .formatted(BASE_URL, BASE_URL));
  }

  @Test
  public void export_whenIdQuestionIsAnswered_valueIsInResponse() {
    createFakeQuestions();
    var fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.idApplicantId())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerIdQuestion(testQuestionBank.idApplicantId(), "011235813")
        .submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
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
  public void export_whenIdQuestionIsRepeated_answersAreCorrectlyNested() {
    createFakeQuestions();
    var fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withHouseholdMembersEnumeratorQuestion()
            .withHouseholdMembersRepeatedQuestion(testQuestionBank.idRepeatedHouseholdMemberId())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerEnumeratorQuestion(ImmutableList.of("taylor"))
        .answerIdQuestion(testQuestionBank.idRepeatedHouseholdMemberId(), "taylor", "011235813")
        .submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertJsonAtApplicationPath(
        ".applicant_household_members",
        """
        {
          "entities" : [ {
            "entity_name" : "taylor",
            "household_member_id" : {
              "id" : "011235813",
              "question_type" : "ID"
            }
          } ],
          "question_type" : "ENUMERATOR"
        }""");
  }

  @Test
  public void export_whenIdQuestionIsNotAnswered_valueInResponseIsNull() {
    createFakeQuestions();
    var fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.idApplicantId())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram).submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
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
            .withQuestion(testQuestionBank.nameApplicantName())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerNameQuestion(testQuestionBank.nameApplicantName(), "Taylor", "Allison", "Swift", "I")
        .submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertJsonAtApplicationPath(
        ".applicant_name",
        """
        {
          "first_name" : "Taylor",
          "last_name" : "Swift",
          "middle_name" : "Allison",
          "question_type" : "NAME",
          "suffix" : "I"
        }""");
  }

  @Test
  public void export_whenNameQuestionIsRepeated_answersAreCorrectlyNested() {
    createFakeQuestions();
    var fakeProgram =
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

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertJsonAtApplicationPath(
        ".applicant_household_members",
        """
        {
          "entities" : [ {
            "entity_name" : "taylor",
            "household_members_name" : {
              "first_name" : "Taylor",
              "last_name" : "Swift",
              "middle_name" : "Allison",
              "question_type" : "NAME",
              "suffix" : "I"
            }
          } ],
          "question_type" : "ENUMERATOR"
        }""");
  }

  @Test
  public void export_whenNameQuestionOnlyRequiredFieldsAreAnswered_unansweredFieldIsNull() {
    createFakeQuestions();
    var fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.nameApplicantName())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerNameQuestion(testQuestionBank.nameApplicantName(), "Taylor", "", "Swift", "")
        .submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertJsonAtApplicationPath(
        ".applicant_name",
        """
        {
          "first_name" : "Taylor",
          "last_name" : "Swift",
          "middle_name" : null,
          "question_type" : "NAME",
          "suffix" : null
        }""");
  }

  @Test
  public void export_whenNameQuestionIsNotAnswered_valueInResponseIsNull() {
    createFakeQuestions();
    var fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.nameApplicantName())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram).submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertJsonAtApplicationPath(
        ".applicant_name",
        """
        {
          "first_name" : null,
          "last_name" : null,
          "middle_name" : null,
          "question_type" : "NAME",
          "suffix" : null
        }""");
  }

  @Test
  public void export_whenNumberQuestionIsAnswered_valueIsInResponse() {
    createFakeQuestions();
    var fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.numberApplicantJugglingNumber())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerNumberQuestion(testQuestionBank.numberApplicantJugglingNumber(), 4200)
        .submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertJsonAtApplicationPath(
        ".number_of_items_applicant_can_juggle",
        """
        {
          "number" : 4200,
          "question_type" : "NUMBER"
        }""");
  }

  @Test
  public void export_whenNumberQuestionIsRepeated_answersAreCorrectlyNested() {
    createFakeQuestions();
    var fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withHouseholdMembersEnumeratorQuestion()
            .withHouseholdMembersRepeatedQuestion(
                testQuestionBank.numberRepeatedHouseholdMemberNumber())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerEnumeratorQuestion(ImmutableList.of("taylor"))
        .answerNumberQuestion(testQuestionBank.numberRepeatedHouseholdMemberNumber(), "taylor", 13L)
        .submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertJsonAtApplicationPath(
        ".applicant_household_members",
        """
        {
          "entities" : [ {
            "entity_name" : "taylor",
            "household_member_favorite_number" : {
              "number" : 13,
              "question_type" : "NUMBER"
            }
          } ],
          "question_type" : "ENUMERATOR"
        }""");
  }

  @Test
  public void export_whenNumberQuestionIsNotAnswered_valueInResponseIsNull() {
    createFakeQuestions();
    var fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.numberApplicantJugglingNumber())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram).submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
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
            .withQuestion(testQuestionBank.phoneApplicantPhone())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerPhoneQuestion(testQuestionBank.phoneApplicantPhone(), "US", "(555) 867-5309")
        .submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
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
  public void export_whenPhoneQuestionIsRepeated_answersAreCorrectlyNested() {
    createFakeQuestions();
    var fakeProgram =
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

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertJsonAtApplicationPath(
        ".applicant_household_members",
        """
        {
          "entities" : [ {
            "entity_name" : "taylor",
            "household_member_cell" : {
              "phone_number" : "+15551331313",
              "question_type" : "PHONE"
            }
          } ],
          "question_type" : "ENUMERATOR"
        }""");
  }

  @Test
  public void export_whenPhoneQuestionIsNotAnswered_valueInResponseIsNull() {
    createFakeQuestions();
    var fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.phoneApplicantPhone())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram).submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
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
            .withQuestion(testQuestionBank.radioApplicantFavoriteSeason())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerRadioButtonQuestion(testQuestionBank.radioApplicantFavoriteSeason(), 3L /* summer */)
        .submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
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
  public void export_whenRadioButtonQuestionIsRepeated_answersAreCorrectlyNested() {
    createFakeQuestions();
    var fakeProgram =
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

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertJsonAtApplicationPath(
        ".applicant_household_members",
        """
        {
          "entities" : [ {
            "entity_name" : "taylor",
            "household_member_favorite_precipitation" : {
              "question_type" : "SINGLE_SELECT",
              "selection" : "snow"
            }
          } ],
          "question_type" : "ENUMERATOR"
        }""");
  }

  @Test
  public void export_whenRadioButtonQuestionIsNotAnswered_valueInResponseIsNull() {
    createFakeQuestions();
    var fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.radioApplicantFavoriteSeason())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram).submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
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
            .withQuestion(testQuestionBank.textApplicantFavoriteColor())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerTextQuestion(testQuestionBank.textApplicantFavoriteColor(), "red 💖")
        .submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertJsonAtApplicationPath(
        ".applicant_favorite_color",
        """
        {
          "question_type" : "TEXT",
          "text" : "red 💖"
        }""");
  }

  @Test
  public void export_whenTextQuestionIsRepeated_answersAreCorrectlyNested() {
    createFakeQuestions();
    var fakeProgram =
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

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertJsonAtApplicationPath(
        ".applicant_household_members",
        """
        {
          "entities" : [ {
            "entity_name" : "taylor",
            "household_member_favorite_shape" : {
              "question_type" : "TEXT",
              "text" : "circle"
            }
          } ],
          "question_type" : "ENUMERATOR"
        }""");
  }

  @Test
  public void export_whenTextQuestionIsNotAnswered_valueInResponseIsNull() {
    createFakeQuestions();
    var fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.textApplicantFavoriteColor())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram).submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
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
        FakeProgramBuilder.newActiveProgram()
            .withHouseholdMembersEnumeratorQuestion()
            .withHouseholdMembersRepeatedQuestion(
                testQuestionBank.textRepeatedApplicantHouseholdMemberFavoriteShape())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerEnumeratorQuestion(ImmutableList.of())
        .submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
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
        FakeProgramBuilder.newActiveProgram()
            .withHouseholdMembersEnumeratorQuestion()
            .withHouseholdMembersRepeatedQuestion(
                testQuestionBank.textRepeatedApplicantHouseholdMemberFavoriteShape())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerEnumeratorQuestion(ImmutableList.of("carly rae", "tswift"))
        .answerTextQuestion(
            testQuestionBank.textRepeatedApplicantHouseholdMemberFavoriteShape(),
            "tswift",
            "hearts")
        .answerTextQuestion(
            testQuestionBank.textRepeatedApplicantHouseholdMemberFavoriteShape(),
            "carly rae",
            "stars")
        .submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
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
        FakeProgramBuilder.newActiveProgram()
            .withHouseholdMembersEnumeratorQuestion()
            .withHouseholdMembersRepeatedQuestion(
                testQuestionBank.textRepeatedApplicantHouseholdMemberFavoriteShape())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerEnumeratorQuestion(ImmutableList.of("carly rae", "tswift"))
        .submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
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
            .withHouseholdMembersRepeatedQuestion(
                testQuestionBank.textRepeatedApplicantHouseholdMemberFavoriteShape())
            .withHouseholdMembersJobsNestedEnumeratorQuestion()
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerEnumeratorQuestion(ImmutableList.of("carly rae", "tswift"))
        .submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
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
            .withHouseholdMembersRepeatedQuestion(
                testQuestionBank.textRepeatedApplicantHouseholdMemberFavoriteShape())
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
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
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
            .withHouseholdMembersRepeatedQuestion(
                testQuestionBank.textRepeatedApplicantHouseholdMemberFavoriteShape())
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
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
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
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerTextQuestion(testQuestionBank.textApplicantFavoriteColor(), "red")
        .submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
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
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertValueAtPath("revision_state", "CURRENT");
  }

  @Test
  public void export_whenApplicationIsObsolete_revisionStateIsObsolete() {
    var fakeProgram = FakeProgramBuilder.newActiveProgram().build();
    FakeApplicationFiller.newFillerFor(fakeProgram).submit().markObsolete();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            fakeProgram.getProgramDefinition(),
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
            SubmittedApplicationFilter.EMPTY);
    ResultAsserter resultAsserter = new ResultAsserter(resultJsonString);

    resultAsserter.assertValueAtPath("revision_state", "OBSOLETE");
  }

  @Test
  public void export_whenQuestionIsAddedToProgram_itIsInResponseForAllApplications()
      throws ProgramNotFoundException {
    createFakeQuestions();
    ProgramModel fakeProgram =
        FakeProgramBuilder.newActiveProgram()
            .withQuestion(testQuestionBank.numberApplicantJugglingNumber())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerNumberQuestion(testQuestionBank.numberApplicantJugglingNumber(), 3)
        .submit();

    ProgramModel updatedFakeProgram =
        FakeProgramBuilder.newDraftOf(fakeProgram)
            .withQuestion(testQuestionBank.emailApplicantEmail())
            .build();
    FakeApplicationFiller.newFillerFor(updatedFakeProgram)
        .answerNumberQuestion(testQuestionBank.numberApplicantJugglingNumber(), 4)
        .answerEmailQuestion(testQuestionBank.emailApplicantEmail(), "test@test.com")
        .submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);

    String resultJsonString =
        exporter.export(
            updatedFakeProgram.getProgramDefinition(),
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
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
            .withQuestion(testQuestionBank.numberApplicantJugglingNumber())
            .withQuestion(testQuestionBank.emailApplicantEmail())
            .build();
    FakeApplicationFiller.newFillerFor(fakeProgram)
        .answerNumberQuestion(testQuestionBank.numberApplicantJugglingNumber(), 3)
        .answerEmailQuestion(testQuestionBank.emailApplicantEmail(), "test@test.com")
        .submit();

    ProgramModel updatedProgram =
        FakeProgramBuilder.removeBlockWithQuestion(
                fakeProgram, testQuestionBank.emailApplicantEmail())
            .build();
    FakeApplicationFiller.newFillerFor(updatedProgram)
        .answerNumberQuestion(testQuestionBank.numberApplicantJugglingNumber(), 4)
        .submit();

    JsonExporterService exporter = instanceOf(JsonExporterService.class);
    String resultJsonString =
        exporter.export(
            updatedProgram.getProgramDefinition(),
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
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
            .withQuestion(testQuestionBank.numberApplicantJugglingNumber())
            .build();
    ProgramModel fakeProgramB =
        FakeProgramBuilder.newActiveProgram("fake program B")
            .withQuestion(testQuestionBank.emailApplicantEmail())
            .build();

    // Fill out both programs
    FakeApplicationFiller.newFillerFor(fakeProgramA, applicant)
        .answerNumberQuestion(testQuestionBank.numberApplicantJugglingNumber(), 3)
        .submit();
    FakeApplicationFiller.newFillerFor(fakeProgramB, applicant)
        .answerEmailQuestion(testQuestionBank.emailApplicantEmail(), "test@test.com")
        .submit();

    String programBResultJson =
        exporter.export(
            fakeProgramB.getProgramDefinition(),
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
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
            .withQuestion(testQuestionBank.numberApplicantJugglingNumber())
            .build();

    // Without updating the application, re-export it
    String updatedProgramBResultJson =
        exporter.export(
            updatedFakeProgramB.getProgramDefinition(),
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
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
            .withQuestion(testQuestionBank.dropdownApplicantIceCream())
            .build();

    // Add new question option
    QuestionDefinition questionDefinition =
        testQuestionBank.dropdownApplicantIceCream().getQuestionDefinition();
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
        .answerDropdownQuestion(
            testQuestionBank.dropdownApplicantIceCream(), 5L) // new "mint" option
        .submit();

    String resultJsonString =
        exporter.export(
            updatedFakeProgram.getProgramDefinition(),
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
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

    private void assertValueAtPath(String path, String value) {
      assertValueAtPath(0, path, value);
    }

    private void assertValueAtPath(int resultNumber, String path, String value) {
      Path jsonPath = Path.create("$[" + resultNumber + "]." + path);
      assertThat(resultJson.readString(jsonPath).get()).isEqualTo(value);
    }

    private void assertValueAtPath(String path, Long value) {
      assertValueAtPath(0, path, value);
    }

    private void assertValueAtPath(int resultNumber, String path, Long value) {
      Path jsonPath = Path.create("$[" + resultNumber + "]." + path);
      assertThat(resultJson.readLong(jsonPath).get()).isEqualTo(value);
    }

    private void assertNullValueAtPath(String path) {
      assertNullValueAtPath(0, path);
    }

    private void assertNullValueAtPath(int resultNumber, String path) {
      Path pathToTest = Path.create("$[" + resultNumber + "]." + path);
      assertThat(resultJson.hasNullValueAtPath(pathToTest)).isTrue();
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
