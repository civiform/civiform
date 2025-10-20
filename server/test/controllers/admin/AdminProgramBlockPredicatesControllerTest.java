package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.OK;
import static support.FakeRequestBuilder.fakeRequest;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import models.ProgramModel;
import org.codehaus.plexus.util.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import play.data.FormFactory;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.test.Helpers;
import repository.ResetPostgres;
import repository.VersionRepository;
import services.program.ProgramService;
import services.program.predicate.PredicateGenerator;
import services.program.predicate.PredicateUseCase;
import services.question.QuestionService;
import services.settings.SettingsManifest;
import support.ProgramBuilder;
import views.admin.programs.ProgramPredicateConfigureView;
import views.admin.programs.ProgramPredicatesEditView;
import views.admin.programs.predicates.EditConditionPartialView;
import views.admin.programs.predicates.EditPredicatePageView;
import views.admin.programs.predicates.EditSubconditionPartialView;
import views.admin.programs.predicates.FailedRequestPartialView;

@RunWith(JUnitParamsRunner.class)
public class AdminProgramBlockPredicatesControllerTest extends ResetPostgres {
  private static final Request EDIT_CONDITION_REQUEST =
      fakeRequestBuilder().bodyForm(ImmutableMap.of("conditionId", "1")).build();
  private static final Request EDIT_SUBCONDITION_REQUEST =
      fakeRequestBuilder()
          .bodyForm(ImmutableMap.of("conditionId", "1", "subconditionId", "1"))
          .build();
  private ProgramModel programWithThreeBlocks;
  private SettingsManifest settingsManifest;

  private AdminProgramBlockPredicatesController controller;

  @Before
  public void setup() {
    settingsManifest = mock(SettingsManifest.class);
    controller =
        new AdminProgramBlockPredicatesController(
            instanceOf(PredicateGenerator.class),
            instanceOf(ProgramService.class),
            instanceOf(QuestionService.class),
            instanceOf(ProgramPredicatesEditView.class),
            instanceOf(ProgramPredicateConfigureView.class),
            instanceOf(EditPredicatePageView.class),
            instanceOf(EditConditionPartialView.class),
            instanceOf(EditSubconditionPartialView.class),
            instanceOf(FailedRequestPartialView.class),
            instanceOf(FormFactory.class),
            instanceOf(RequestChecker.class),
            instanceOf(ProfileUtils.class),
            instanceOf(VersionRepository.class),
            settingsManifest);
    programWithThreeBlocks =
        ProgramBuilder.newDraftProgram("first program")
            .withBlock("Screen 1")
            .withRequiredQuestion(testQuestionBank.nameApplicantName())
            .withBlock("Screen 2")
            .withRequiredCorrectedAddressQuestion(testQuestionBank.addressApplicantAddress())
            .withRequiredQuestion(testQuestionBank.dropdownApplicantIceCream())
            .withRequiredQuestion(testQuestionBank.checkboxApplicantKitchenTools())
            .withBlock("Screen 3")
            .withRequiredQuestion(testQuestionBank.textApplicantFavoriteColor())
            .build();
  }

  @Test
  @Parameters({"true", "false"})
  public void editVisibility_withNonExistentProgram_notFound(boolean expandedFormLogicEnabled) {
    when(settingsManifest.getExpandedFormLogicEnabled(fakeRequest()))
        .thenReturn(expandedFormLogicEnabled);
    assertThatThrownBy(
            () ->
                controller.editVisibility(
                    fakeRequest(), /* programId= */ 1, /* blockDefinitionId= */ 1))
        .isInstanceOf(NotChangeableException.class);
  }

  @Test
  @Parameters({"true", "false"})
  public void editEligibility_withNonExistentProgram_notFound(boolean expandedFormLogicEnabled) {
    when(settingsManifest.getExpandedFormLogicEnabled(fakeRequest()))
        .thenReturn(expandedFormLogicEnabled);
    assertThatThrownBy(
            () ->
                controller.editEligibility(
                    fakeRequest(), /* programId= */ 1, /* blockDefinitionId= */ 1))
        .isInstanceOf(NotChangeableException.class);
  }

  @Test
  public void updateEligibilityMessage_withNonExistentProgram_notFound() {
    assertThatThrownBy(
            () -> controller.updateEligibilityMessage(fakeRequest(), 1, /* blockDefinitionId= */ 1))
        .isInstanceOf(controllers.admin.NotChangeableException.class)
        .hasMessage("Program 1 is not a Draft.");
  }

  @Test
  @Parameters({"true", "false"})
  public void editVisibility_withInvalidBlock_notFound(boolean expandedFormLogicEnabled) {
    when(settingsManifest.getExpandedFormLogicEnabled(fakeRequest()))
        .thenReturn(expandedFormLogicEnabled);
    ProgramModel program = ProgramBuilder.newDraftProgram().build();

    Result result =
        controller.editVisibility(fakeRequest(), program.id, /* blockDefinitionId= */ 543L);

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  @Parameters({"true", "false"})
  public void editEligibility_withInvalidBlock_notFound(boolean expandedFormLogicEnabled) {
    when(settingsManifest.getExpandedFormLogicEnabled(fakeRequest()))
        .thenReturn(expandedFormLogicEnabled);
    ProgramModel program = ProgramBuilder.newDraftProgram().build();

    Result result =
        controller.editEligibility(fakeRequest(), program.id, /* blockDefinitionId= */ 543L);

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void updateEligibilityMessage_withInvalidBlock_notFound() {
    ProgramModel program = ProgramBuilder.newDraftProgram().build();

    Result result =
        controller.updateEligibilityMessage(
            fakeRequest(), program.id, /* blockDefinitionId= */ 543L);

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  @Parameters({"true", "false"})
  public void editVisibility_withActiveProgram_throws(boolean expandedFormLogicEnabled) {
    when(settingsManifest.getExpandedFormLogicEnabled(fakeRequest()))
        .thenReturn(expandedFormLogicEnabled);
    Long programId = resourceCreator.insertActiveProgram("active program").id;
    assertThatThrownBy(
            () -> controller.editVisibility(fakeRequest(), programId, /* blockDefinitionId= */ 1))
        .isInstanceOf(NotChangeableException.class);
  }

  @Test
  @Parameters({"true", "false"})
  public void editEligibility_withActiveProgram_throws(boolean expandedFormLogicEnabled) {
    when(settingsManifest.getExpandedFormLogicEnabled(fakeRequest()))
        .thenReturn(expandedFormLogicEnabled);
    Long programId = resourceCreator.insertActiveProgram("active program").id;
    assertThatThrownBy(
            () -> controller.editEligibility(fakeRequest(), programId, /* blockDefinitionId= */ 1))
        .isInstanceOf(NotChangeableException.class);
  }

  @Test
  public void edit_withFirstBlock_displaysEmptyList() {
    Result result =
        controller.editVisibility(
            fakeRequest(), programWithThreeBlocks.id, /* blockDefinitionId= */ 1L);

    assertThat(result.status()).isEqualTo(OK);
    String content = Helpers.contentAsString(result);
    assertThat(content).contains("Visibility condition for Screen 1");
    assertThat(content).contains("This screen is always shown.");
    assertThat(content)
        .contains(
            "There are no available questions with which to set a visibility condition for this"
                + " screen.");
  }

  @Test
  public void editEligibility_withFirstBlock_displaysFirstBlock() {
    Result result =
        controller.editEligibility(
            fakeRequest(), programWithThreeBlocks.id, /* blockDefinitionId= */ 1L);

    assertThat(result.status()).isEqualTo(OK);
    String content = Helpers.contentAsString(result);
    assertThat(content).contains("Eligibility condition for Screen 1");
    assertThat(content).contains("This screen does not have any eligibility conditions");
    assertThat(content).contains("Admin ID: applicant name");
    assertThat(content).contains("what is your name?");
  }

  @Test
  public void edit_withThirdBlock_displaysQuestionsFromFirstAndSecondBlock() {
    Result result =
        controller.editVisibility(
            fakeRequest(), programWithThreeBlocks.id, /* blockDefinitionId= */ 3L);

    assertThat(result.status()).isEqualTo(OK);
    String content = Helpers.contentAsString(result);
    assertThat(content).contains("Visibility condition for Screen 3");
    assertThat(content).contains("Admin ID: applicant name");
    assertThat(content).contains("what is your name?");
    assertThat(content).contains("Admin ID: applicant address");
    assertThat(content).contains("What is your address?");
    assertThat(content).contains("Admin ID: applicant ice cream");
    assertThat(content).contains("Select your favorite ice cream flavor");
    assertThat(content).doesNotContain("Admin ID: applicant favorite color");
    assertThat(content).doesNotContain("What is your favorite color?");
  }

  @Test
  public void update_activeProgram_throws() {
    Long programId = resourceCreator.insertActiveProgram("active program").id;
    assertThatThrownBy(
            () -> controller.updateVisibility(fakeRequest(), programId, /* blockDefinitionId= */ 1))
        .isInstanceOf(NotChangeableException.class);
  }

  @Test
  public void updateEligibility_activeProgram_throws() {
    Long programId = resourceCreator.insertActiveProgram("active program").id;
    assertThatThrownBy(
            () ->
                controller.updateEligibility(fakeRequest(), programId, /* blockDefinitionId= */ 1))
        .isInstanceOf(NotChangeableException.class);
  }

  @Test
  public void destroy_activeProgram_throws() {
    Long programId = resourceCreator.insertActiveProgram("active program").id;
    assertThatThrownBy(() -> controller.destroyVisibility(programId, /* blockDefinitionId= */ 1))
        .isInstanceOf(NotChangeableException.class);
  }

  @Test
  public void destroyEligibility_activeProgram_throws() {
    Long programId = resourceCreator.insertActiveProgram("active program").id;
    assertThatThrownBy(() -> controller.destroyEligibility(programId, /* blockDefinitionId= */ 1))
        .isInstanceOf(NotChangeableException.class);
  }

  @Test
  public void hxEditCondition_expandedLogicDisabled_notFound() {
    when(settingsManifest.getExpandedFormLogicEnabled(any())).thenReturn(false);

    Result result =
        controller.hxEditCondition(
            EDIT_CONDITION_REQUEST,
            programWithThreeBlocks.id,
            /* blockDefinitionId= */ 1L,
            PredicateUseCase.ELIGIBILITY.name());

    assertThat(result.status()).isEqualTo(NOT_FOUND);
    assertThat(Helpers.contentAsString(result)).contains("Expanded form logic is not enabled.");
  }

  @Test
  public void hxEditCondition_eligibility_withFirstBlock_displaysFirstBlockQuestions() {
    when(settingsManifest.getExpandedFormLogicEnabled(any())).thenReturn(true);
    Result result =
        controller.hxEditCondition(
            EDIT_CONDITION_REQUEST,
            programWithThreeBlocks.id,
            /* blockDefinitionId= */ 1L,
            PredicateUseCase.ELIGIBILITY.name());

    assertThat(result.status()).isEqualTo(OK);
    String content = Helpers.contentAsString(result);
    assertThat(content).contains("what is your name?");
  }

  @Test
  public void hxEditCondition_visibility_withThirdBlock_displaysFirstAndSecondBlockQuestions() {
    when(settingsManifest.getExpandedFormLogicEnabled(any())).thenReturn(true);
    Result result =
        controller.hxEditCondition(
            EDIT_CONDITION_REQUEST,
            programWithThreeBlocks.id,
            /* blockDefinitionId= */ 3L,
            PredicateUseCase.VISIBILITY.name());

    assertThat(result.status()).isEqualTo(OK);
    String content = Helpers.contentAsString(result);
    assertThat(content).contains("what is your name?");
    assertThat(content).contains("What is your address?");
    assertThat(content).contains("Select your favorite ice cream flavor");
    assertThat(content).doesNotContain("What is your favorite color?");
  }

  @Test
  public void hxEditSubcondition_expandedLogicDisabled_notFound() {
    when(settingsManifest.getExpandedFormLogicEnabled(any())).thenReturn(false);

    Result result =
        controller.hxEditSubcondition(
            EDIT_SUBCONDITION_REQUEST,
            programWithThreeBlocks.id,
            /* blockDefinitionId= */ 1L,
            PredicateUseCase.ELIGIBILITY.name());

    assertThat(result.status()).isEqualTo(NOT_FOUND);
    assertThat(Helpers.contentAsString(result)).contains("Expanded form logic is not enabled.");
  }

  @Test
  public void hxEditSubcondition_eligibility_withFirstBlock_displaysFirstBlockQuestions() {
    when(settingsManifest.getExpandedFormLogicEnabled(any())).thenReturn(true);
    Result result =
        controller.hxEditSubcondition(
            EDIT_SUBCONDITION_REQUEST,
            programWithThreeBlocks.id,
            /* blockDefinitionId= */ 1L,
            PredicateUseCase.ELIGIBILITY.name());
    assertThat(result.status()).isEqualTo(OK);
    String content = Helpers.contentAsString(result);
    assertThat(content).contains("what is your name?");
  }

  @Test
  public void hxEditSubcondition_visibility_withThirdBlock_displaysFirstAndSecondBlockQuestions() {
    when(settingsManifest.getExpandedFormLogicEnabled(any())).thenReturn(true);
    Result result =
        controller.hxEditSubcondition(
            EDIT_SUBCONDITION_REQUEST,
            programWithThreeBlocks.id,
            /* blockDefinitionId= */ 3L,
            PredicateUseCase.VISIBILITY.name());

    assertThat(result.status()).isEqualTo(OK);
    String content = Helpers.contentAsString(result);
    assertThat(content).contains("what is your name?");
    assertThat(content).contains("What is your address?");
    assertThat(content).contains("Select your favorite ice cream flavor");
    assertThat(content).doesNotContain("What is your favorite color?");
  }

  @Test
  public void hxEditCondition_noForm_returnsOkAndDisplaysAlert() {
    when(settingsManifest.getExpandedFormLogicEnabled(any())).thenReturn(true);
    Result result =
        controller.hxEditCondition(
            fakeRequestBuilder().build(),
            programWithThreeBlocks.id,
            /* blockDefinitionId= */ 3L,
            PredicateUseCase.VISIBILITY.name());

    assertThat(result.status()).isEqualTo(OK);
    String content = Helpers.contentAsString(result);
    assertThat(content).contains("We are experiencing a system error");
  }

  @Test
  public void hxEditCondition_invalidProgramId_returnsOkAndDisplaysAlert() {
    when(settingsManifest.getExpandedFormLogicEnabled(any())).thenReturn(true);
    Result result =
        controller.hxEditCondition(
            fakeRequest(),
            /* programId= */ 1,
            /* blockDefinitionId= */ 3L,
            PredicateUseCase.VISIBILITY.name());

    assertThat(result.status()).isEqualTo(OK);
    String content = Helpers.contentAsString(result);
    assertThat(content).contains("We are experiencing a system error");
  }

  @Test
  public void hxEditCondition_invalidBlockId_returnsOkAndDisplaysAlert() {
    when(settingsManifest.getExpandedFormLogicEnabled(any())).thenReturn(true);
    Result result =
        controller.hxEditCondition(
            fakeRequest(),
            programWithThreeBlocks.id,
            /* blockDefinitionId= */ 543L,
            PredicateUseCase.VISIBILITY.name());

    assertThat(result.status()).isEqualTo(OK);
    String content = Helpers.contentAsString(result);
    assertThat(content).contains("We are experiencing a system error");
  }

  @Test
  public void hxEditCondition_invalidPredicateUseCase_returnsOkAndDisplaysAlert() {
    when(settingsManifest.getExpandedFormLogicEnabled(any())).thenReturn(true);
    Result result =
        controller.hxEditCondition(
            fakeRequest(),
            programWithThreeBlocks.id,
            /* blockDefinitionId= */ 3L,
            /* predicateUseCase= */ "RANDOM_USE_CASE");

    assertThat(result.status()).isEqualTo(OK);
    String content = Helpers.contentAsString(result);
    assertThat(content).contains("We are experiencing a system error");
  }

  @Test
  public void hxEditSubcondition_withAddressQuestionId_isSelected() {
    when(settingsManifest.getExpandedFormLogicEnabled(any())).thenReturn(true);
    Result result =
        controller.hxEditSubcondition(
            fakeRequestBuilder()
                .bodyForm(
                    ImmutableMap.of(
                        "conditionId",
                        "1",
                        "subconditionId",
                        "1",
                        "condition-1-subcondition-1-question",
                        String.valueOf(testQuestionBank.addressApplicantAddress().id)))
                .build(),
            programWithThreeBlocks.id,
            /* blockDefinitionId= */ 3L,
            PredicateUseCase.VISIBILITY.name());

    assertThat(result.status()).isEqualTo(OK);
    String content = Helpers.contentAsString(result);
    assertThat(StringUtils.deleteWhitespace(content))
        .contains(
            String.format(
                "<optionvalue=\"%d\"selected=\"selected\">",
                testQuestionBank.addressApplicantAddress().id));
    // Verify only scalars applicable to the selected question are shown
    assertThat(content).contains("service area");
    assertThat(content)
        .doesNotContain(ImmutableList.of("street", "first name", "date", "currency"));
  }

  @Test
  public void hxEditSubcondition_malformedQuestionId_selectsDefaults() {
    when(settingsManifest.getExpandedFormLogicEnabled(any())).thenReturn(true);
    Result result =
        controller.hxEditSubcondition(
            fakeRequestBuilder()
                .bodyForm(
                    ImmutableMap.of(
                        "conditionId",
                        "1",
                        "subconditionId",
                        "1",
                        "condition-1-subcondition-1-INVALIDQuestionId",
                        String.valueOf(testQuestionBank.addressApplicantAddress().id)))
                .build(),
            programWithThreeBlocks.id,
            /* blockDefinitionId= */ 3L,
            PredicateUseCase.VISIBILITY.name());

    assertThat(result.status()).isEqualTo(OK);
    String content = Helpers.contentAsString(result);
    // Verify that the question with id addressApplicantAddress is not selected.
    assertThat(StringUtils.deleteWhitespace(content))
        .doesNotContain(
            String.format(
                "<optionvalue=\"%d\"selected=\"selected\">",
                testQuestionBank.addressApplicantAddress().id));
    // Without a question selected, verify the form is in its default state with 4 default inputs
    // (question, scalar, operator value) and
    // scalar/operator/value disabled.
    assertThat(
            StringUtils.countMatches(
                StringUtils.deleteWhitespace(content), "<optionselected=\"selected\">"))
        .isEqualTo(4);
    assertThat(StringUtils.countMatches(content, "disabled=\"disabled\"")).isEqualTo(3);
  }
}
