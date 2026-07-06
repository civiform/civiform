package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static play.mvc.Http.Status.BAD_REQUEST;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.OK;
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
import services.program.ProgramBlockDefinitionNotFoundException;
import services.program.ProgramNotFoundException;
import services.program.ProgramQuestionDefinition;
import services.program.ProgramService;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionDefinitionConfig;
import services.question.types.TextQuestionDefinition;
import support.ProgramBuilder;
import views.admin.programs.ProgramBlocksView;

public class AdminProgramBlockQuestionsControllerTest extends ResetPostgres {

  private AdminProgramBlockQuestionsController controller;
  private ProgramService programService;

  @Before
  public void setUp() {
    controller = instanceOf(AdminProgramBlockQuestionsController.class);
    programService = instanceOf(ProgramService.class);
  }

  @Test
  public void create_addOldRevisionAddsLatestRevision() throws UnsupportedQuestionTypeException {
    // Setup.
    QuestionDefinition nameQuestion = testQuestionBank.nameApplicantName().getQuestionDefinition();
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
  public void hxCreateEnumerator_addsNewEnumeratorQuestionToBlock()
      throws ProgramBlockDefinitionNotFoundException {

    ProgramBuilder programBuilder = ProgramBuilder.newDraftProgram();
    ProgramModel program = programBuilder.withEnumeratorBlock().build();

    Request request =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.of(
                    "entityType", "Pets",
                    "questionName", "pets enumerator",
                    "questionText", "List your pets.",
                    "questionHelpText", "help text"))
            .build();

    Result result = controller.hxCreateEnumerator(request, program.id, 1);

    assertThat(result.status()).isEqualTo(OK);

    assertThat(contentAsString(result)).contains("id=\"repeated-set-question-section-heading\"");
    assertThat(contentAsString(result))
        .contains("data-testid=\"question-admin-name-pets enumerator\"");
  }

  @Test
  public void hxCreateEnumerator_withExistingInitialQuestion_createsCopyAndAddsBothToBlock()
      throws ProgramBlockDefinitionNotFoundException, ProgramNotFoundException {
    QuestionDefinition initialQuestion =
        testQuestionBank.nameApplicantName().getQuestionDefinition();
    ProgramModel program = ProgramBuilder.newDraftProgram().withEnumeratorBlock().build();

    Request request =
        fakeRequestBuilder()
            .addCiviFormSetting("ENUMERATOR_IMPROVEMENTS_ENABLED", "true")
            .bodyForm(
                ImmutableMap.of(
                    "entityType", "Pets",
                    "questionName", "pets enumerator",
                    "questionText", "List your pets.",
                    "questionHelpText", "help text",
                    "initialQuestionId", String.valueOf(initialQuestion.getId())))
            .build();

    Result result = controller.hxCreateEnumerator(request, program.id, 1);

    assertThat(result.status()).isEqualTo(OK);

    BlockDefinition blockAfter =
        programService.getFullProgramDefinition(program.id).getBlockDefinition(1L);
    assertThat(blockAfter.programQuestionDefinitions()).hasSize(2);
    QuestionDefinition enumeratorOnBlock =
        blockAfter.programQuestionDefinitions().get(0).getQuestionDefinition();
    QuestionDefinition initialOnBlock =
        blockAfter.programQuestionDefinitions().get(1).getQuestionDefinition();
    assertThat(enumeratorOnBlock.isEnumerator()).isTrue();
    // A copy of the initial question was created with " -_- a" suffix.
    assertThat(initialOnBlock.getName()).isEqualTo(initialQuestion.getName() + " -_- a");
    assertThat(initialOnBlock.getEnumeratorId()).contains(enumeratorOnBlock.getId());
    // The enumerator links to the persisted initial question copy.
    assertThat(enumeratorOnBlock.getEnumeratorInitialQuestionId()).contains(initialOnBlock.getId());
  }

  @Test
  public void hxCreateEnumerator_withNewlyCreatedInitialQuestion_updatesInPlaceAndAddsBothToBlock()
      throws ProgramBlockDefinitionNotFoundException, ProgramNotFoundException {
    // Mimic a question just created via the "Create new question" flow — a draft that hasn't
    // been attached to any enumerator yet.
    QuestionDefinition initialQuestion =
        testQuestionBank
            .maybeSave(
                new TextQuestionDefinition(
                    QuestionDefinitionConfig.builder()
                        .setName("newly-created-initial-question")
                        .setDescription("desc")
                        .setQuestionText(LocalizedStrings.of(Locale.US, "?"))
                        .build()),
                LifecycleStage.DRAFT)
            .getQuestionDefinition();
    ProgramModel program = ProgramBuilder.newDraftProgram().withEnumeratorBlock().build();

    Request request =
        fakeRequestBuilder()
            .addCiviFormSetting("ENUMERATOR_IMPROVEMENTS_ENABLED", "true")
            .bodyForm(
                ImmutableMap.of(
                    "entityType", "Pets",
                    "questionName", "pets enumerator",
                    "questionText", "List your pets.",
                    "questionHelpText", "help text",
                    "initialQuestionId", String.valueOf(initialQuestion.getId()),
                    "initialQuestionWasNewlyCreated", "true"))
            .build();

    Result result = controller.hxCreateEnumerator(request, program.id, 1);

    assertThat(result.status()).isEqualTo(OK);

    BlockDefinition blockAfter =
        programService.getFullProgramDefinition(program.id).getBlockDefinition(1L);
    assertThat(blockAfter.programQuestionDefinitions()).hasSize(2);
    QuestionDefinition enumeratorOnBlock =
        blockAfter.programQuestionDefinitions().get(0).getQuestionDefinition();
    QuestionDefinition initialOnBlock =
        blockAfter.programQuestionDefinitions().get(1).getQuestionDefinition();
    assertThat(enumeratorOnBlock.isEnumerator()).isTrue();
    // Update-in-place: same id, no " -_- a" suffix on the name.
    assertThat(initialOnBlock.getId()).isEqualTo(initialQuestion.getId());
    assertThat(initialOnBlock.getName()).isEqualTo(initialQuestion.getName());
    assertThat(initialOnBlock.getEnumeratorId()).contains(enumeratorOnBlock.getId());
    assertThat(enumeratorOnBlock.getEnumeratorInitialQuestionId()).contains(initialOnBlock.getId());
  }

  @Test
  public void hxCreateEnumerator_withUnknownInitialQuestionId_returnsNotFound() {
    ProgramModel program = ProgramBuilder.newDraftProgram().withEnumeratorBlock().build();

    Request request =
        fakeRequestBuilder()
            .addCiviFormSetting("ENUMERATOR_IMPROVEMENTS_ENABLED", "true")
            .bodyForm(
                ImmutableMap.of(
                    "entityType", "Pets",
                    "questionName", "pets enumerator",
                    "questionText", "List your pets.",
                    "questionHelpText", "help text",
                    "initialQuestionId", "99999"))
            .build();

    Result result = controller.hxCreateEnumerator(request, program.id, 1);

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void
      hxCreateEnumerator_withMissingInitialQuestionIdButNewlyCreatedFlag_skipsInitialQuestionHandling()
          throws ProgramBlockDefinitionNotFoundException, ProgramNotFoundException {
    ProgramModel program = ProgramBuilder.newDraftProgram().withEnumeratorBlock().build();

    Request request =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.of(
                    "entityType", "Pets",
                    "questionName", "pets enumerator",
                    "questionText", "List your pets.",
                    "questionHelpText", "help text",
                    "initialQuestionWasNewlyCreated", "true"))
            .build();

    Result result = controller.hxCreateEnumerator(request, program.id, 1);

    assertThat(result.status()).isEqualTo(OK);

    BlockDefinition blockAfter =
        programService.getFullProgramDefinition(program.id).getBlockDefinition(1L);
    // Only the enumerator was added; no initial question handling occurred.
    assertThat(blockAfter.programQuestionDefinitions()).hasSize(1);
    QuestionDefinition enumeratorOnBlock =
        blockAfter.programQuestionDefinitions().get(0).getQuestionDefinition();
    assertThat(enumeratorOnBlock.isEnumerator()).isTrue();
    assertThat(enumeratorOnBlock.getEnumeratorInitialQuestionId()).isEmpty();
  }

  @Test
  public void hxCreateEnumerator_withIncompleteForm_returnsEnumeratorFormWithErrorMessage()
      throws ProgramBlockDefinitionNotFoundException {

    ProgramBuilder programBuilder = ProgramBuilder.newDraftProgram();
    ProgramModel program = programBuilder.withEnumeratorBlock().build();

    Request request =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.of(
                    "entityType", "Pets",
                    "questionName", "pets enumerator", // Missing questionText
                    "questionHelpText", "help text"))
            .build();

    Result result = controller.hxCreateEnumerator(request, program.id, 1);

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result))
        .contains("<div id=\"enumerator-setup\" class=\"maxw-mobile-lg\">");
    assertThat(contentAsString(result)).contains("Error: Question text cannot be blank.");
  }

  @Test
  public void hxSelectInitialQuestion_returnsSlotFragmentAndCloseTrigger() {
    QuestionDefinition nameQuestion = testQuestionBank.nameApplicantName().getQuestionDefinition();
    ProgramModel program = ProgramBuilder.newDraftProgram().withEnumeratorBlock().build();

    Request request =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.of(
                    "question-" + nameQuestion.getId(), String.valueOf(nameQuestion.getId())))
            .build();

    Result result = controller.hxSelectInitialQuestion(request, program.id, 1);

    assertThat(result.status()).isEqualTo(OK);
    // The initial question card
    assertThat(contentAsString(result))
        .contains("<div data-testid=\"question-admin-name-" + nameQuestion.getName());
    assertThat(contentAsString(result))
        .contains(
            """
            <input type="hidden" name="initialQuestionId" value="%s">\
            """
                .formatted(nameQuestion.getId()));
    assertThat(result.header("HX-Trigger-After-Swap")).hasValue("closeQuestionBank");
  }

  @Test
  public void hxSelectInitialQuestion_doesNotAddQuestionToBlock()
      throws ProgramBlockDefinitionNotFoundException {
    QuestionDefinition nameQuestion = testQuestionBank.nameApplicantName().getQuestionDefinition();
    ProgramModel program = ProgramBuilder.newDraftProgram().withEnumeratorBlock().build();

    Request request =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.of(
                    "question-" + nameQuestion.getId(), String.valueOf(nameQuestion.getId())))
            .build();

    Result result = controller.hxSelectInitialQuestion(request, program.id, 1);

    assertThat(result.status()).isEqualTo(OK);
    program.refresh();
    BlockDefinition blockAfter = program.getProgramDefinition().getBlockDefinition(1L);
    assertThat(blockAfter.programQuestionDefinitions()).isEmpty();
  }

  @Test
  public void hxSelectInitialQuestion_withNoQuestionInForm_returnsBadRequest() {
    ProgramModel program = ProgramBuilder.newDraftProgram().withEnumeratorBlock().build();

    Request request = fakeRequestBuilder().bodyForm(ImmutableMap.of()).build();

    Result result = controller.hxSelectInitialQuestion(request, program.id, 1);

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void hxSelectInitialQuestion_withUnknownQuestionId_returnsNotFound() {
    ProgramModel program = ProgramBuilder.newDraftProgram().withEnumeratorBlock().build();

    Request request =
        fakeRequestBuilder().bodyForm(ImmutableMap.of("question-99999", "99999")).build();

    Result result = controller.hxSelectInitialQuestion(request, program.id, 1);

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void hxSelectInitialQuestion_withActiveProgram_throws() {
    Long programId = resourceCreator.insertActiveProgram("active program").id;
    assertThatThrownBy(
            () -> controller.hxSelectInitialQuestion(fakeRequest(), programId, /* blockId= */ 1))
        .isInstanceOf(NotChangeableException.class);
  }

  @Test
  public void hxClearInitialQuestionSlot_returnsEmptySlot() {
    ProgramModel program = ProgramBuilder.newDraftProgram().withEnumeratorBlock().build();

    Result result = controller.hxClearInitialQuestionSlot(fakeRequest(), program.id, 1);

    assertThat(result.status()).isEqualTo(OK);
    String content = contentAsString(result);
    assertThat(content).contains("id=\"initial-question-slot\"");
    assertThat(content).contains("Add question");
  }

  @Test
  public void hxClearInitialQuestionSlot_withActiveProgram_throws() {
    Long programId = resourceCreator.insertActiveProgram("active program").id;
    assertThatThrownBy(
            () -> controller.hxClearInitialQuestionSlot(fakeRequest(), programId, /* blockId= */ 1))
        .isInstanceOf(NotChangeableException.class);
  }

  @Test
  public void create_withActiveProgram_throws() {
    Long programId = resourceCreator.insertActiveProgram("active program").id;
    assertThatThrownBy(() -> controller.create(fakeRequest(), programId, /* blockId= */ 1))
        .isInstanceOf(NotChangeableException.class);
  }

  @Test
  public void delete_withActiveProgram_throws() {
    Long programId = resourceCreator.insertActiveProgram("active program").id;
    assertThatThrownBy(
            () ->
                controller.delete(
                    fakeRequest(),
                    programId,
                    /* blockDefinitionId= */ 1,
                    /* questionDefinitionId= */ 1))
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
    QuestionDefinition nameQuestion = testQuestionBank.nameApplicantName().getQuestionDefinition();
    QuestionDefinition addressQuestion =
        testQuestionBank.addressApplicantAddress().getQuestionDefinition();
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
    QuestionDefinition nameQuestion = testQuestionBank.nameApplicantName().getQuestionDefinition();
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
