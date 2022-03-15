package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.SEE_OTHER;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeRequest;

import com.google.common.collect.ImmutableMap;
import models.Program;
import models.Question;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.test.Helpers;
import repository.ResetPostgres;
import services.program.ProgramDefinition;
import services.question.QuestionService;
import services.question.exceptions.InvalidUpdateException;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import support.ProgramBuilder;

public class AdminProgramBlocksControllerTest extends ResetPostgres {

  private AdminProgramBlocksController controller;
  private QuestionService questionService;

  @Before
  public void setup() {
    controller = instanceOf(AdminProgramBlocksController.class);
    questionService = instanceOf(QuestionService.class);
  }

  @Test
  public void index_withInvalidProgram_notFound() {
    Result result = controller.index(1L);

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void index_withProgram_redirectsToEdit() {
    Program program = ProgramBuilder.newDraftProgram().build();

    Result result = controller.index(program.id);

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation())
        .hasValue(routes.AdminProgramBlocksController.edit(program.id, 1L).url());
  }

  @Test
  public void create_withInvalidProgram_notFound() {
    Request request = fakeRequest().build();
    Result result = controller.create(request, 1L);

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void create_withProgram_addsBlock() {
    Request request = fakeRequest().build();
    Program program = ProgramBuilder.newDraftProgram().build();
    Result result = controller.create(request, program.id);

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation())
        .hasValue(routes.AdminProgramBlocksController.edit(program.id, 2L).url());

    program.refresh();
    assertThat(program.getProgramDefinition().blockDefinitions()).hasSize(2);
  }

  @Test
  public void edit_withInvalidProgram_notFound() {
    Request request = fakeRequest().build();
    Result result = controller.edit(request, 1L, 1L);

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void edit_withInvalidBlock_notFound() {
    Program program = ProgramBuilder.newDraftProgram().build();
    Request request = fakeRequest().build();
    Result result = controller.edit(request, program.id, 2L);

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void edit_withProgram_OK()
      throws UnsupportedQuestionTypeException, InvalidUpdateException {
    Program program = ProgramBuilder.newActiveProgram().build();
    Question appName = testQuestionBank.applicantName();
    appName.save();
    Request request = addCSRFToken(fakeRequest()).build();
    Result result = controller.edit(request, program.id, 1L);

    assertThat(result.status()).isEqualTo(OK);
    assertThat(Helpers.contentAsString(result))
        .contains(appName.getQuestionDefinition().getDescription());

    QuestionDefinition questionDefinition =
        new QuestionDefinitionBuilder(appName.getQuestionDefinition())
            .setDescription("NEW DESCRIPTION")
            .build();

    questionService.update(questionDefinition);
    request = addCSRFToken(fakeRequest()).build();
    result = controller.edit(request, program.id, 1L);

    assertThat(result.status()).isEqualTo(OK);
    assertThat(Helpers.contentAsString(result))
        .doesNotContain(appName.getQuestionDefinition().getDescription());
    assertThat(Helpers.contentAsString(result)).contains(questionDefinition.getDescription());
  }

  @Test
  public void update_withInvalidProgram_notFound() {
    Request request =
        fakeRequest()
            .bodyForm(ImmutableMap.of("name", "name", "description", "description"))
            .build();

    Result result = controller.update(request, 1L, 1L);

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void update_withInvalidBlockId_notFound() {
    Program program = ProgramBuilder.newDraftProgram().build();
    Request request =
        fakeRequest()
            .bodyForm(ImmutableMap.of("name", "name", "description", "description"))
            .build();

    Result result = controller.update(request, program.id, 2L);

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void update_overwritesExistingBlock() {
    ProgramDefinition program = ProgramBuilder.newDraftProgram().buildDefinition();
    Request request =
        fakeRequest()
            .bodyForm(ImmutableMap.of("name", "updated name", "description", "updated description"))
            .build();

    Result result =
        controller.update(request, program.id(), program.getBlockDefinitionByIndex(0).get().id());

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation())
        .hasValue(
            routes.AdminProgramBlocksController.edit(
                    program.id(), program.getBlockDefinitionByIndex(0).get().id())
                .url());

    Result redirectResult =
        controller.edit(
            addCSRFToken(fakeRequest()).build(),
            program.id(),
            program.getBlockDefinitionByIndex(0).get().id());
    assertThat(contentAsString(redirectResult)).contains("updated name");
  }

  @Test
  public void destroy_withInvalidProgram_notFound() {
    Result result = controller.destroy(1L, 1L);

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void destroy_programWithTwoBlocks_redirects() {
    Program program = ProgramBuilder.newDraftProgram().withBlock().withBlock().build();
    Result result = controller.destroy(program.id, 1L);

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation())
        .hasValue(routes.AdminProgramBlocksController.index(program.id).url());
  }

  @Test
  public void destroy_lastBlock_notFound() {
    Program program = ProgramBuilder.newDraftProgram().build();
    Result result = controller.destroy(program.id, 1L);

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }
}
