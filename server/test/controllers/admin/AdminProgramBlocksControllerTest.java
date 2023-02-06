package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import services.LocalizedStrings;
import services.program.ProgramDefinition;
import services.question.QuestionService;
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
    Result result = controller.index(/*programId =*/ 1L);

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void index_withProgram_redirectsToEdit() {
    Program program = ProgramBuilder.newDraftProgram().build();

    Result result = controller.index(program.id);

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation())
        .hasValue(
            routes.AdminProgramBlocksController.edit(program.id, /*blockDefinitionId =*/ 1L).url());
  }

  @Test
  public void readOnlyIndex_readOnly_redirectsToShow() {
    Program program = ProgramBuilder.newActiveProgram().build();

    Result result = controller.readOnlyIndex(program.id);

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation())
        .hasValue(
            routes.AdminProgramBlocksController.show(program.id, /*blockDefinitionId =*/ 1L).url());
  }

  @Test
  public void create_withInvalidProgram_notFound() {
    Request request = fakeRequest().build();
    assertThatThrownBy(() -> controller.create(request, /*programId =*/ 1L))
        .isInstanceOf(NotChangeableException.class);
  }

  @Test
  public void create_withProgram_addsBlock() {
    Request request = fakeRequest().build();
    Program program = ProgramBuilder.newDraftProgram().build();
    Result result = controller.create(request, program.id);

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation())
        .hasValue(
            routes.AdminProgramBlocksController.edit(program.id, /*blockDefinitionId =*/ 2L).url());

    program.refresh();
    assertThat(program.getProgramDefinition().blockDefinitions()).hasSize(2);
  }

  @Test
  public void create_withProgram_addsRepeatedBlock() {
    Program program =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredQuestion(testQuestionBank.applicantHouseholdMembers())
            .withBlock()
            .withRequiredQuestion(testQuestionBank.applicantFavoriteColor())
            .build();
    Request request = fakeRequest().bodyForm(ImmutableMap.of("enumeratorId", "1")).build();
    Result result = controller.create(request, program.id);

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    // Ensures we're redirected to the newly created block rather than the last
    // block in the program (see issue #1885).
    assertThat(result.redirectLocation())
        .hasValue(
            routes.AdminProgramBlocksController.edit(program.id, /*blockDefinitionId =*/ 3L).url());

    program.refresh();
    assertThat(program.getProgramDefinition().blockDefinitions()).hasSize(3);
  }

  @Test
  public void show_withNoneActiveProgram_throwsNotViewableException() throws Exception {
    Request request = addCSRFToken(Helpers.fakeRequest()).build();
    Program program = ProgramBuilder.newDraftProgram("test program").build();

    assertThatThrownBy(() -> controller.show(request, program.id, /*blockId =*/ 1L))
        .isInstanceOf(NotViewableException.class);
  }

  @Test
  public void show_withInvalidProgram_notFound() {
    Request request = fakeRequest().build();
    assertThatThrownBy(() -> controller.show(request, /*programId =*/ 1L, /*blockId =*/ 1L))
        .isInstanceOf(NotViewableException.class);
  }

  @Test
  public void show_withInvalidBlock_notFound() {
    Program program = ProgramBuilder.newActiveProgram().build();
    Request request = fakeRequest().build();
    Result result = controller.show(request, program.id, /*blockId =*/ 2L);

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void show() throws Exception {
    Program program =
        ProgramBuilder.newActiveProgram("Public name", "Public description")
            // Override only admin name and description to distinguish from applicant-visible
            // name/description.
            .withName("Admin name")
            .withDescription("Admin description")
            .build();
    Question applicantName = testQuestionBank.applicantName();
    applicantName.save();
    Request request = addCSRFToken(fakeRequest()).build();
    Result result = controller.show(request, program.id, /*blockId =*/ 1L);

    assertThat(result.status()).isEqualTo(OK);
    String html = Helpers.contentAsString(result);
    assertThat(html)
        .contains("Public name")
        .contains("Public description")
        .contains("Admin description")
        // Similar to program index page we don't show admin name.
        .doesNotContain("Admin name");
    // The read only program viewing does not include the question bank
    assertThat(html)
        .doesNotContain(applicantName.getQuestionDefinition().getQuestionText().getDefault());
  }

  @Test
  public void edit_withInvalidProgram_notFound() {
    Request request = fakeRequest().build();
    assertThatThrownBy(() -> controller.edit(request, /*programId =*/ 1L, /*blockId =*/ 1L))
        .isInstanceOf(NotChangeableException.class);
  }

  @Test
  public void edit_withInvalidBlock_notFound() {
    Program program = ProgramBuilder.newDraftProgram().build();
    Request request = fakeRequest().build();
    Result result = controller.edit(request, program.id, /*blockId =*/ 2L);

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void edit() throws Exception {
    Program program =
        ProgramBuilder.newDraftProgram("Public name", "Public description")
            // Override only admin name and description to distinguish from applicant-visible
            // name/description.
            .withName("Admin name")
            .withDescription("Admin description")
            .build();
    Question applicantName = testQuestionBank.applicantName();
    applicantName.save();
    Request request = addCSRFToken(fakeRequest()).build();
    Result result = controller.edit(request, program.id, /*blockId =*/ 1L);

    assertThat(result.status()).isEqualTo(OK);
    String html = Helpers.contentAsString(result);
    assertThat(html).contains(applicantName.getQuestionDefinition().getQuestionText().getDefault());
    assertThat(html)
        .contains(applicantName.getQuestionDefinition().getQuestionHelpText().getDefault());
    assertThat(html).contains("Admin ID: " + applicantName.getQuestionDefinition().getName());
    assertThat(html)
        .contains("Public name")
        .contains("Public description")
        .contains("Admin description")
        // Similar to program index page we don't show admin name.
        .doesNotContain("Admin name");

    QuestionDefinition otherQuestionDef =
        new QuestionDefinitionBuilder(applicantName.getQuestionDefinition())
            .setQuestionText(LocalizedStrings.withDefaultValue("NEW QUESTION TEXT"))
            .build();

    questionService.update(otherQuestionDef);
    request = addCSRFToken(fakeRequest()).build();
    result = controller.edit(request, program.id, /*blockId =*/ 1L);

    assertThat(result.status()).isEqualTo(OK);
    assertThat(Helpers.contentAsString(result))
        .doesNotContain(applicantName.getQuestionDefinition().getQuestionText().getDefault());
    assertThat(Helpers.contentAsString(result))
        .contains(otherQuestionDef.getQuestionText().getDefault());
  }

  @Test
  public void update_withInvalidProgram_notFound() {
    Request request =
        fakeRequest()
            .bodyForm(ImmutableMap.of("name", "name", "description", "description"))
            .build();

    assertThatThrownBy(() -> controller.update(request, /*programId =*/ 1L, /*blockId =*/ 1L))
        .isInstanceOf(NotChangeableException.class);
  }

  @Test
  public void update_withInvalidBlockId_notFound() {
    Program program = ProgramBuilder.newDraftProgram().build();
    Request request =
        fakeRequest()
            .bodyForm(ImmutableMap.of("name", "name", "description", "description"))
            .build();

    Result result = controller.update(request, program.id, /*blockId =*/ 2L);

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
        controller.update(
            request,
            program.id(),
            program.getBlockDefinitionByIndex(/*blockIndex =*/ 0).get().id());

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation())
        .hasValue(
            routes.AdminProgramBlocksController.edit(
                    program.id(), program.getBlockDefinitionByIndex(/*blockIndex =*/ 0).get().id())
                .url());

    Result redirectResult =
        controller.edit(
            addCSRFToken(fakeRequest()).build(),
            program.id(),
            program.getBlockDefinitionByIndex(/*blockIndex =*/ 0).get().id());
    assertThat(contentAsString(redirectResult)).contains("updated name");
  }

  @Test
  public void destroy_withInvalidProgram_notFound() {
    assertThatThrownBy(() -> controller.destroy(/*programId =*/ 1L, /*blockId =*/ 1L))
        .isInstanceOf(NotChangeableException.class);
  }

  @Test
  public void destroy_programWithTwoBlocks_redirects() {
    Program program = ProgramBuilder.newDraftProgram().withBlock().withBlock().build();
    Result result = controller.destroy(program.id, /*blockId =*/ 1L);

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation())
        .hasValue(routes.AdminProgramBlocksController.index(program.id).url());
  }

  @Test
  public void destroy_lastBlock_notFound() {
    Program program = ProgramBuilder.newDraftProgram().build();
    Result result = controller.destroy(program.id, /*blockId =*/ 1L);

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }
}
