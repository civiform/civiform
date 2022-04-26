package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.SEE_OTHER;
import static play.test.Helpers.fakeRequest;

import com.google.common.collect.ImmutableMap;
import models.Program;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http;
import play.mvc.Result;
import play.test.Helpers;
import repository.ResetPostgres;
import support.ProgramBuilder;

public class AdminProgramBlockPredicatesControllerTest extends ResetPostgres {
  private Program programWithThreeBlocks;

  private AdminProgramBlockPredicatesController controller;

  @Before
  public void setup() {
    controller = instanceOf(AdminProgramBlockPredicatesController.class);
    programWithThreeBlocks =
        ProgramBuilder.newDraftProgram()
            .withBlock("Screen 1")
            .withRequiredQuestion(testQuestionBank.applicantName())
            .withBlock("Screen 2")
            .withRequiredQuestion(testQuestionBank.applicantAddress())
            .withRequiredQuestion(testQuestionBank.applicantIceCream())
            .withRequiredQuestion(testQuestionBank.applicantKitchenTools())
            .withBlock("Screen 3")
            .withRequiredQuestion(testQuestionBank.applicantFavoriteColor())
            .build();
  }

  @Test
  public void edit_withInvalidProgram_notFound() {
    Http.Request request = fakeRequest().build();

    Result result = controller.edit(request, 1L, 1L);

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void edit_withInvalidBlock_notFound() {
    Http.Request request = addCSRFToken(fakeRequest()).build();
    Program program = ProgramBuilder.newDraftProgram().build();

    Result result = controller.edit(request, program.id, 543L);

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void edit_withFirstBlock_displaysEmptyList() {
    Http.Request request = addCSRFToken(fakeRequest()).build();

    Result result = controller.edit(request, programWithThreeBlocks.id, 1L);

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
  public void edit_withThirdBlock_displaysQuestionsFromFirstAndSecondBlock() {
    Http.Request request = addCSRFToken(fakeRequest()).build();

    Result result = controller.edit(request, programWithThreeBlocks.id, 3L);

    assertThat(result.status()).isEqualTo(OK);
    String content = Helpers.contentAsString(result);
    assertThat(content).contains("Visibility condition for Screen 3");
    assertThat(content).contains("applicant name");
    assertThat(content).contains("applicant address");
    assertThat(content).contains("applicant ice cream");
    assertThat(content).doesNotContain("applicant favorite color");
  }

  @Test
  public void update_withValidFormData_savesNewPredicate() {
    // Test that the edit page does not display a saved predicate beforehand.
    Result editBeforeResult =
        controller.edit(addCSRFToken(fakeRequest()).build(), programWithThreeBlocks.id, 3L);
    assertThat(Helpers.contentAsString(editBeforeResult)).contains("This screen is always shown.");

    Http.Request request =
        fakeRequest()
            .bodyForm(
                ImmutableMap.of(
                    "predicateAction",
                    "HIDE_BLOCK",
                    "questionId",
                    String.valueOf(testQuestionBank.applicantName().id),
                    "scalar",
                    "FIRST_NAME",
                    "operator",
                    "EQUAL_TO",
                    "predicateValue",
                    "Hello"))
            .build();

    Result result = controller.update(request, programWithThreeBlocks.id, 3L);

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation())
        .hasValue(
            routes.AdminProgramBlockPredicatesController.edit(programWithThreeBlocks.id, 3L).url());
    assertThat(result.flash().get("error")).isEmpty();
    assertThat(result.flash().get("success").get()).contains("Saved visibility condition");

    // For some reason the above result has an empty contents. So we test the new content of the
    // edit page manually.
    Result redirectResult =
        controller.edit(addCSRFToken(fakeRequest()).build(), programWithThreeBlocks.id, 3L);
    assertThat(Helpers.contentAsString(redirectResult))
        .doesNotContain("This screen is always shown.");
  }

  @Test
  public void update_withSingleSelectQuestion() {
    Http.Request request =
        fakeRequest()
            .bodyForm(
                ImmutableMap.of(
                    "predicateAction",
                    "HIDE_BLOCK",
                    "questionId",
                    String.valueOf(testQuestionBank.applicantIceCream().id),
                    "scalar",
                    "SELECTION",
                    "operator",
                    "IN",
                    "predicateValues[]",
                    "1"))
            .build();

    Result result = controller.update(request, programWithThreeBlocks.id, 3L);

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation())
        .hasValue(
            routes.AdminProgramBlockPredicatesController.edit(programWithThreeBlocks.id, 3L).url());
    assertThat(result.flash().get("error")).isEmpty();
    assertThat(result.flash().get("success").get()).contains("Saved visibility condition");
  }

  @Test
  public void update_withMultiSelectQuestion() {
    Http.Request request =
        fakeRequest()
            .bodyForm(
                ImmutableMap.of(
                    "predicateAction",
                    "SHOW_BLOCK",
                    "questionId",
                    String.valueOf(testQuestionBank.applicantKitchenTools().id),
                    "scalar",
                    "SELECTIONS",
                    "operator",
                    "ANY_OF",
                    "predicateValues[]",
                    "1"))
            .build();

    Result result = controller.update(request, programWithThreeBlocks.id, 3L);

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation())
        .hasValue(
            routes.AdminProgramBlockPredicatesController.edit(programWithThreeBlocks.id, 3L).url());
    assertThat(result.flash().get("error")).isEmpty();
    assertThat(result.flash().get("success").get()).contains("Saved visibility condition");

    // For some reason the above result has an empty contents. So we test the new content of the
    // edit page manually.
    Result redirectResult =
        controller.edit(addCSRFToken(fakeRequest()).build(), programWithThreeBlocks.id, 3L);
    assertThat(Helpers.contentAsString(redirectResult))
        .doesNotContain("This screen is always shown.");
  }

  @Test
  public void update_withMissingRequiredFields_doesNotSavePredicate() {
    Http.Request request =
        fakeRequest()
            .bodyForm(
                ImmutableMap.of(
                    "predicateAction",
                    "",
                    "questionId",
                    "",
                    "scalar",
                    "",
                    "operator",
                    "",
                    "predicateValue",
                    ""))
            .build();

    Result result = controller.update(request, programWithThreeBlocks.id, 3L);

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation())
        .hasValue(
            routes.AdminProgramBlockPredicatesController.edit(programWithThreeBlocks.id, 3L).url());
    assertThat(result.flash().get("error").get()).contains("Did not save visibility condition");
    assertThat(result.flash().get("success")).isEmpty();

    // For some reason the above result has an empty contents. So we test the new content of the
    // edit page manually.
    Result redirectResult =
        controller.edit(addCSRFToken(fakeRequest()).build(), programWithThreeBlocks.id, 3L);
    assertThat(Helpers.contentAsString(redirectResult)).contains("This screen is always shown.");
  }

  @Test
  public void update_withInvalidOperator_doesNotSavePredicate() {
    Http.Request request =
        fakeRequest()
            .bodyForm(
                ImmutableMap.of(
                    "predicateAction",
                    "HIDE_BLOCK",
                    "questionId",
                    "1",
                    "scalar",
                    "FIRST_NAME",
                    "operator",
                    "GREATER_THAN",
                    "predicateValue",
                    "Hello"))
            .build();

    Result result = controller.update(request, programWithThreeBlocks.id, 3L);

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation())
        .hasValue(
            routes.AdminProgramBlockPredicatesController.edit(programWithThreeBlocks.id, 3L).url());
    assertThat(result.flash().get("error").get()).contains("Did not save visibility condition");
    assertThat(result.flash().get("error").get())
        .contains("Cannot use operator \"is greater than\" on scalar \"first name\".");
    assertThat(result.flash().get("success")).isEmpty();

    // For some reason the above result has an empty contents. So we test the new content of the
    // edit page manually.
    Result redirectResult =
        controller.edit(addCSRFToken(fakeRequest()).build(), programWithThreeBlocks.id, 3L);
    assertThat(Helpers.contentAsString(redirectResult)).contains("This screen is always shown.");
  }

  @Test
  public void destroy_removesPredicate() {
    // First add a predicate and assert its presence.
    Http.Request request =
        fakeRequest()
            .bodyForm(
                ImmutableMap.of(
                    "predicateAction",
                    "HIDE_BLOCK",
                    "questionId",
                    String.valueOf(testQuestionBank.applicantName().id),
                    "scalar",
                    "FIRST_NAME",
                    "operator",
                    "EQUAL_TO",
                    "predicateValue",
                    "Hello"))
            .build();
    Result resultWithPredicate = controller.update(request, programWithThreeBlocks.id, 3L);
    assertThat(resultWithPredicate.flash().get("success").get())
        .contains("Saved visibility condition");

    // Then use the destroy endpoint and confirm the predicate's absence.
    Result resultWithoutPredicate = controller.destroy(programWithThreeBlocks.id, 3L);

    assertThat(resultWithoutPredicate.status()).isEqualTo(SEE_OTHER);
    assertThat(resultWithoutPredicate.flash().get("success").get())
        .contains("Removed the visibility condition for this screen.");

    // For some reason the above result has an empty contents. So we test the new content of the
    // edit page manually.
    Result redirectResult =
        controller.edit(addCSRFToken(fakeRequest()).build(), programWithThreeBlocks.id, 3L);
    assertThat(Helpers.contentAsString(redirectResult)).contains("This screen is always shown.");
  }
}
