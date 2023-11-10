package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.fakeRequest;

import com.google.common.collect.ImmutableList;
import models.ProgramModel;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http;
import play.mvc.Result;
import play.test.Helpers;
import repository.ResetPostgres;
import services.applicant.question.Scalar;
import services.program.predicate.Operator;
import services.program.predicate.PredicateGenerator;
import services.program.predicate.PredicateValue;
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
            .withRequiredQuestion(testQuestionBank.applicantName())
            .withBlock("Screen 2")
            .withRequiredCorrectedAddressQuestion(testQuestionBank.applicantAddress())
            .withRequiredQuestion(testQuestionBank.applicantIceCream())
            .withRequiredQuestion(testQuestionBank.applicantKitchenTools())
            .withBlock("Screen 3")
            .withRequiredQuestion(testQuestionBank.applicantFavoriteColor())
            .build();
  }

  @Test
  public void edit_withNonExistantProgram_notFound() {
    assertThatThrownBy(
            () ->
                controller.editVisibility(
                    fakeRequest().build(), /* programId= */ 1, /* blockDefinitionId= */ 1))
        .isInstanceOf(NotChangeableException.class);
  }

  @Test
  public void editEligibility_withNonExistantProgram_notFound() {
    assertThatThrownBy(
            () ->
                controller.editEligibility(
                    fakeRequest().build(), /* programId= */ 1, /* blockDefinitionId= */ 1))
        .isInstanceOf(NotChangeableException.class);
  }

  @Test
  public void edit_withInvalidBlock_notFound() {
    Http.Request request = addCSRFToken(fakeRequest()).build();
    ProgramModel program = ProgramBuilder.newDraftProgram().build();

    Result result = controller.editEligibility(request, program.id, 543L);

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void editEligibility_withInvalidBlock_notFound() {
    Http.Request request = addCSRFToken(fakeRequest()).build();
    ProgramModel program = ProgramBuilder.newDraftProgram().build();

    Result result = controller.editEligibility(request, program.id, 543L);

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void edit_withActiveProgram_throws() {
    Long programId = resourceCreator.insertActiveProgram("active program").id;
    assertThatThrownBy(
            () ->
                controller.editVisibility(
                    fakeRequest().build(), programId, /* blockDefinitionId= */ 1))
        .isInstanceOf(NotChangeableException.class);
  }

  @Test
  public void editEligibility_withActiveProgram_throws() {
    Long programId = resourceCreator.insertActiveProgram("active program").id;
    assertThatThrownBy(
            () ->
                controller.editEligibility(
                    fakeRequest().build(), programId, /* blockDefinitionId= */ 1))
        .isInstanceOf(NotChangeableException.class);
  }

  @Test
  public void edit_withFirstBlock_displaysEmptyList() {
    Http.Request request = addCSRFToken(fakeRequest()).build();

    Result result = controller.editVisibility(request, programWithThreeBlocks.id, 1L);

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
    Http.Request request = addCSRFToken(fakeRequest()).build();

    Result result = controller.editEligibility(request, programWithThreeBlocks.id, 1L);

    assertThat(result.status()).isEqualTo(OK);
    String content = Helpers.contentAsString(result);
    assertThat(content).contains("Eligibility condition for Screen 1");
    assertThat(content).contains("This screen is always eligible");
    assertThat(content).contains("Admin ID: applicant name");
    assertThat(content).contains("what is your name?");
  }

  @Test
  public void edit_withThirdBlock_displaysQuestionsFromFirstAndSecondBlock() {
    Http.Request request = addCSRFToken(fakeRequest()).build();

    Result result = controller.editVisibility(request, programWithThreeBlocks.id, 3L);

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
            () ->
                controller.updateVisibility(
                    fakeRequest().build(), programId, /* blockDefinitionId= */ 1))
        .isInstanceOf(NotChangeableException.class);
  }

  @Test
  public void updateEligibility_activeProgram_throws() {
    Long programId = resourceCreator.insertActiveProgram("active program").id;
    assertThatThrownBy(
            () ->
                controller.updateEligibility(
                    fakeRequest().build(), programId, /* blockDefinitionId= */ 1))
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
  public void testParsePredicateValue_currency() {
    PredicateValue got =
        PredicateGenerator.parsePredicateValue(
            Scalar.CURRENCY_CENTS, Operator.EQUAL_TO, "100.01", ImmutableList.of());

    assertThat(
            got.toDisplayString(testQuestionBank.applicantMonthlyIncome().getQuestionDefinition()))
        .isEqualTo("$100.01");
  }
}
