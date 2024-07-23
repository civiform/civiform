package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static play.mvc.Http.Status.SEE_OTHER;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.stubMessagesApi;
import static support.FakeRequestBuilder.fakeRequest;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import models.LifecycleStage;
import models.ProgramModel;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http.Request;
import play.mvc.Result;
import repository.ResetPostgres;
import services.LocalizedStrings;
import services.program.BlockDefinition;
import services.program.InvalidQuestionPositionException;
import services.program.ProgramQuestionDefinition;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import support.ProgramBuilder;
import views.admin.programs.ProgramBlocksView;

public class AdminProgramBlockQuestionsControllerTest extends ResetPostgres {

  private AdminProgramBlockQuestionsController controller;

  @Before
  public void setUp() {
    controller = instanceOf(AdminProgramBlockQuestionsController.class);
  }

  @Test
  public void create_addOldRevisionAddsLatestRevision() throws UnsupportedQuestionTypeException {
    // Setup.
    QuestionDefinition nameQuestion = testQuestionBank.applicantName().getQuestionDefinition();
    Long activeId = nameQuestion.getId();
    Long draftId = activeId + 100000;
    QuestionDefinition toUpdate =
        new QuestionDefinitionBuilder(nameQuestion)
            .setId(draftId)
            .setQuestionText(LocalizedStrings.withDefaultValue("draft version"))
            .build();
    testQuestionBank.maybeSave(toUpdate, LifecycleStage.DRAFT);
    ProgramBuilder programBuilder = ProgramBuilder.newDraftProgram();
    ProgramModel program = programBuilder.withBlock("block1").build();

    // Execute.
    Request request =
        fakeRequestBuilder()
            .call(
                controllers.admin.routes.AdminProgramBlockQuestionsController.create(program.id, 1))
            .langCookie(Locale.forLanguageTag("es-US"), stubMessagesApi())
            .bodyForm(ImmutableMap.of("question-", activeId.toString()))
            .build();
    Result result = controller.create(request, program.id, 1);

    // Verify.
    assertThat(result.status()).withFailMessage(contentAsString(result)).isEqualTo(SEE_OTHER);
    program.refresh();
    assertThat(program.getProgramDefinition().hasQuestion(toUpdate)).isTrue();
    assertThat(program.getProgramDefinition().hasQuestion(nameQuestion)).isFalse();
  }

  @Test
  public void create_withActiveProgram_throws() {
    Long programId = resourceCreator.insertActiveProgram("active program").id;
    assertThatThrownBy(() -> controller.create(fakeRequest(), programId, /* blockId= */ 1))
        .isInstanceOf(NotChangeableException.class);
  }

  @Test
  public void destroy_withActiveProgram_throws() {
    Long programId = resourceCreator.insertActiveProgram("active program").id;
    assertThatThrownBy(
            () ->
                controller.destroy(
                    programId, /* blockDefinitionId= */ 1, /* questionDefinitionId= */ 1))
        .isInstanceOf(NotChangeableException.class);
  }

  @Test
  public void setOptional_withActiveProgram_throws() {
    Long programId = resourceCreator.insertActiveProgram("active program").id;
    assertThatThrownBy(
            () ->
                controller.setOptional(
                    fakeRequest(),
                    programId,
                    /* blockDefinitionId= */ 1,
                    /* questionDefinitionId= */ 1))
        .isInstanceOf(NotChangeableException.class);
  }

  @Test
  public void move_changesOrderOfQuestions() throws Exception {
    // Setup.
    QuestionDefinition nameQuestion = testQuestionBank.applicantName().getQuestionDefinition();
    QuestionDefinition addressQuestion =
        testQuestionBank.applicantAddress().getQuestionDefinition();
    ProgramBuilder programBuilder = ProgramBuilder.newDraftProgram();
    ProgramModel program =
        programBuilder
            .withBlock("block1")
            .withOptionalQuestion(nameQuestion)
            .withOptionalQuestion(addressQuestion)
            .build();
    BlockDefinition block = program.getProgramDefinition().getLastBlockDefinition();

    // Execute. Move "name" question to position 1.
    Request request =
        fakeRequestBuilder()
            .call(
                controllers.admin.routes.AdminProgramBlockQuestionsController.move(
                    program.id, block.id(), nameQuestion.getId()))
            .langCookie(Locale.forLanguageTag("es-US"), stubMessagesApi())
            .bodyForm(ImmutableMap.of(ProgramBlocksView.MOVE_QUESTION_POSITION_FIELD, "1"))
            .build();
    Result result = controller.move(request, program.id, block.id(), nameQuestion.getId());

    // Verify.
    assertThat(result.status()).withFailMessage(contentAsString(result)).isEqualTo(SEE_OTHER);
    program.refresh();
    assertThat(
            program
                .getProgramDefinition()
                .getLastBlockDefinition()
                .programQuestionDefinitions()
                .stream()
                .map(ProgramQuestionDefinition::id))
        .containsExactly(addressQuestion.getId(), nameQuestion.getId());
  }

  @Test
  public void move_invalidPositionInput() throws Exception {
    // Setup.
    QuestionDefinition nameQuestion = testQuestionBank.applicantName().getQuestionDefinition();
    ProgramBuilder programBuilder = ProgramBuilder.newDraftProgram();
    ProgramModel program =
        programBuilder.withBlock("block1").withOptionalQuestion(nameQuestion).build();
    BlockDefinition block = program.getProgramDefinition().getLastBlockDefinition();

    // Missing position value.
    Request requestWithNoPosition =
        fakeRequestBuilder()
            .call(
                controllers.admin.routes.AdminProgramBlockQuestionsController.move(
                    program.id, block.id(), nameQuestion.getId()))
            .langCookie(Locale.forLanguageTag("es-US"), stubMessagesApi())
            .build();
    assertThatThrownBy(
            () ->
                controller.move(
                    requestWithNoPosition, program.id, block.id(), nameQuestion.getId()))
        .isInstanceOf(InvalidQuestionPositionException.class);

    // Position is not a number.
    Request requestWithInvalidPosition =
        fakeRequestBuilder()
            .call(
                controllers.admin.routes.AdminProgramBlockQuestionsController.move(
                    program.id, block.id(), nameQuestion.getId()))
            .langCookie(Locale.forLanguageTag("es-US"), stubMessagesApi())
            .bodyForm(ImmutableMap.of(ProgramBlocksView.MOVE_QUESTION_POSITION_FIELD, "foobar"))
            .build();
    assertThatThrownBy(
            () ->
                controller.move(
                    requestWithInvalidPosition, program.id, block.id(), nameQuestion.getId()))
        .isInstanceOf(InvalidQuestionPositionException.class);
  }
}
