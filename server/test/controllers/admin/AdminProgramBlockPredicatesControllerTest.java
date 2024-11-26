package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.OK;
import static support.FakeRequestBuilder.fakeRequest;

import models.ProgramModel;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Result;
import play.test.Helpers;
import repository.ResetPostgres;
import services.LocalizedStrings;
import services.program.BlockDefinition;
import support.ProgramBuilder;

public class AdminProgramBlockPredicatesControllerTest extends ResetPostgres {
  private ProgramModel programWithThreeBlocks;

  private AdminProgramBlockPredicatesController controller;

  @Before
  public void setup() {
    controller = instanceOf(AdminProgramBlockPredicatesController.class);
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
  public void edit_withNonExistantProgram_notFound() {
    assertThatThrownBy(
            () ->
                controller.editVisibility(
                    fakeRequest(), /* programId= */ 1, /* blockDefinitionId= */ 1))
        .isInstanceOf(NotChangeableException.class);
  }

  @Test
  void updateEligibilityMessage_addsEligibilityMsg() {

    BlockDefinition block =
        BlockDefinition.builder()
            .setId(1)
            .setName("Screen 1")
            .setDescription("Screen 1 description")
            .setLocalizedName(LocalizedStrings.withDefaultValue("Screen 1"))
            .setLocalizedDescription(LocalizedStrings.withDefaultValue("Screen 1 description"))
            .build();

    ProgramModel program = ProgramBuilder.newActiveProgram().withBlock(block).build();

    // Request

    Result result = controller.updateEligibilityMessage(fakeRequest(), program.id, block.id());
    String content = Helpers.contentAsString(result);
    assertThat(content).contains("");
  }

  @Test
  public void editEligibility_withNonExistantProgram_notFound() {
    assertThatThrownBy(
            () ->
                controller.editEligibility(
                    fakeRequest(), /* programId= */ 1, /* blockDefinitionId= */ 1))
        .isInstanceOf(NotChangeableException.class);
  }

  @Test
  public void edit_withInvalidBlock_notFound() {
    ProgramModel program = ProgramBuilder.newDraftProgram().build();

    Result result = controller.editEligibility(fakeRequest(), program.id, 543L);

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void editEligibility_withInvalidBlock_notFound() {
    ProgramModel program = ProgramBuilder.newDraftProgram().build();

    Result result = controller.editEligibility(fakeRequest(), program.id, 543L);

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void edit_withActiveProgram_throws() {
    Long programId = resourceCreator.insertActiveProgram("active program").id;
    assertThatThrownBy(
            () -> controller.editVisibility(fakeRequest(), programId, /* blockDefinitionId= */ 1))
        .isInstanceOf(NotChangeableException.class);
  }

  @Test
  public void editEligibility_withActiveProgram_throws() {
    Long programId = resourceCreator.insertActiveProgram("active program").id;
    assertThatThrownBy(
            () -> controller.editEligibility(fakeRequest(), programId, /* blockDefinitionId= */ 1))
        .isInstanceOf(NotChangeableException.class);
  }

  @Test
  public void edit_withFirstBlock_displaysEmptyList() {
    Result result = controller.editVisibility(fakeRequest(), programWithThreeBlocks.id, 1L);

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
    Result result = controller.editEligibility(fakeRequest(), programWithThreeBlocks.id, 1L);

    assertThat(result.status()).isEqualTo(OK);
    String content = Helpers.contentAsString(result);
    assertThat(content).contains("Eligibility condition for Screen 1");
    assertThat(content).contains("This screen is always eligible");
    assertThat(content).contains("Admin ID: applicant name");
    assertThat(content).contains("what is your name?");
  }

  @Test
  public void edit_withThirdBlock_displaysQuestionsFromFirstAndSecondBlock() {
    Result result = controller.editVisibility(fakeRequest(), programWithThreeBlocks.id, 3L);

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
}
