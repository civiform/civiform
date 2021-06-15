package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.fakeRequest;

import models.Program;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http;
import play.mvc.Result;
import play.test.Helpers;
import repository.WithPostgresContainer;
import support.ProgramBuilder;

public class AdminProgramBlockPredicatesControllerTest extends WithPostgresContainer {

  private AdminProgramBlockPredicatesController controller;

  @Before
  public void setup() {
    controller = instanceOf(AdminProgramBlockPredicatesController.class);
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
    Program program =
        ProgramBuilder.newDraftProgram()
            .withBlock("Block 1")
            .withQuestion(testQuestionBank.applicantName())
            .withBlock("Block 2")
            .withQuestion(testQuestionBank.applicantAddress())
            .build();

    Result result = controller.edit(request, program.id, 1L);

    assertThat(result.status()).isEqualTo(OK);
    String content = Helpers.contentAsString(result);
    assertThat(content).contains("Add a condition to show or hide Block 1");
    assertThat(content).contains("This block is always shown.");
    assertThat(content)
        .contains(
            "There are no available questions with which to set a visibility condition for this"
                + " block.");
  }

  @Test
  public void edit_withThirdBlock_displaysQuestionsFromFirstAndSecondBlock() {
    Http.Request request = addCSRFToken(fakeRequest()).build();
    Program program =
        ProgramBuilder.newDraftProgram()
            .withBlock("Block 1")
            .withQuestion(testQuestionBank.applicantName())
            .withBlock("Block 2")
            .withQuestion(testQuestionBank.applicantAddress())
            .withQuestion(testQuestionBank.applicantIceCream())
            .withBlock("Block 3")
            .withQuestion(testQuestionBank.applicantFavoriteColor())
            .build();

    Result result = controller.edit(request, program.id, 3L);

    assertThat(result.status()).isEqualTo(OK);
    String content = Helpers.contentAsString(result);
    assertThat(content).contains("Add a condition to show or hide Block 3");
    assertThat(content).contains("applicant name");
    assertThat(content).contains("applicant address");
    assertThat(content).contains("applicant ice cream");
    assertThat(content).doesNotContain("applicant favorite color");
  }

  @Test
  public void update_withValidFormData_savesNewPredicate() {}

  @Test
  public void update_withInvalidFormData_doesNotSavePredicate() {}
}
