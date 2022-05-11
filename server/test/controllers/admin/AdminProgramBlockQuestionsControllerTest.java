package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static play.mvc.Http.Status.SEE_OTHER;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.stubMessagesApi;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import models.LifecycleStage;
import models.Program;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http.Request;
import play.mvc.Result;
import repository.ResetPostgres;
import services.LocalizedStrings;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import support.ProgramBuilder;

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
    Program program = programBuilder.withBlock("block1").build();

    // Execute.
    Request request =
        fakeRequest(
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
    assertThrows(
        NotChangeableException.class,
        () -> controller.create(fakeRequest().build(), programId, /* blockId= */ 1));
  }

  @Test
  public void destroy_withActiveProgram_throws() {
    Long programId = resourceCreator.insertActiveProgram("active program").id;
    assertThrows(
        NotChangeableException.class,
        () ->
            controller.destroy(
                programId, /* blockDefinitionId= */ 1, /* questionDefinitionId= */ 1));
  }

  @Test
  public void setOptional_withActiveProgram_throws() {
    Long programId = resourceCreator.insertActiveProgram("active program").id;
    assertThrows(
        NotChangeableException.class,
        () ->
            controller.setOptional(
                fakeRequest().build(),
                programId,
                /* blockDefinitionId= */ 1,
                /* questionDefinitionId= */ 1));
  }
}
