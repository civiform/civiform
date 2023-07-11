package controllers.dev;

import static com.google.common.base.Preconditions.checkNotNull;
import static controllers.dev.seeding.QuestionDefinitions.ADDRESS_QUESTION_DEFINITION;
import static controllers.dev.seeding.QuestionDefinitions.CHECKBOX_QUESTION_DEFINITION;
import static controllers.dev.seeding.QuestionDefinitions.CURRENCY_QUESTION_DEFINITION;
import static controllers.dev.seeding.QuestionDefinitions.DATE_QUESTION_DEFINITION;
import static controllers.dev.seeding.QuestionDefinitions.DROPDOWN_QUESTION_DEFINITION;
import static controllers.dev.seeding.QuestionDefinitions.EMAIL_QUESTION_DEFINITION;
import static controllers.dev.seeding.QuestionDefinitions.ENUMERATOR_QUESTION_DEFINITION;
import static controllers.dev.seeding.QuestionDefinitions.FILE_UPLOAD_QUESTION_DEFINITION;
import static controllers.dev.seeding.QuestionDefinitions.ID_QUESTION_DEFINITION;
import static controllers.dev.seeding.QuestionDefinitions.NUMBER_QUESTION_DEFINITION;
import static controllers.dev.seeding.QuestionDefinitions.PHONE_QUESTION_DEFINITION;
import static controllers.dev.seeding.QuestionDefinitions.RADIO_BUTTON_QUESTION_DEFINITION;
import static controllers.dev.seeding.QuestionDefinitions.STATIC_CONTENT_QUESTION_DEFINITION;
import static controllers.dev.seeding.QuestionDefinitions.TEXT_QUESTION_DEFINITION;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import forms.BlockForm;
import io.ebean.DB;
import io.ebean.Database;
import java.util.ArrayList;
import java.util.Optional;
import models.DisplayMode;
import models.LifecycleStage;
import models.Models;
import models.Version;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;
import services.CiviFormError;
import services.DeploymentType;
import services.ErrorAnd;
import services.applicant.question.Scalar;
import services.program.ActiveAndDraftPrograms;
import services.program.ProgramDefinition;
import services.program.ProgramService;
import services.program.ProgramType;
import services.program.predicate.LeafOperationExpressionNode;
import services.program.predicate.Operator;
import services.program.predicate.PredicateAction;
import services.program.predicate.PredicateDefinition;
import services.program.predicate.PredicateExpressionNode;
import services.program.predicate.PredicateValue;
import services.question.QuestionService;
import services.question.types.QuestionDefinition;
import services.settings.SettingsService;
import tasks.DatabaseSeedTask;
import views.dev.DatabaseSeedView;

/** Controller for seeding the database with test content to develop against. */
public class DatabaseSeedController extends Controller {

  private final DatabaseSeedTask databaseSeedTask;
  private final DatabaseSeedView view;
  private final Database database;
  private final QuestionService questionService;
  private final ProgramService programService;
  private final SettingsService settingsService;
  private final boolean isDevOrStaging;

  @Inject
  public DatabaseSeedController(
      DatabaseSeedTask databaseSeedTask,
      DatabaseSeedView view,
      QuestionService questionService,
      ProgramService programService,
      SettingsService settingsService,
      DeploymentType deploymentType) {
    this.databaseSeedTask = checkNotNull(databaseSeedTask);
    this.view = checkNotNull(view);
    this.database = DB.getDefault();
    this.questionService = checkNotNull(questionService);
    this.programService = checkNotNull(programService);
    this.settingsService = checkNotNull(settingsService);
    this.isDevOrStaging = deploymentType.isDevOrStaging();
  }

  /**
   * Display state of the database in roughly formatted string. Displays a button to generate mock
   * database content and another to clear the database.
   */
  public Result index(Request request) {
    if (!isDevOrStaging) {
      return notFound();
    }
    ActiveAndDraftPrograms activeAndDraftPrograms = programService.getActiveAndDraftPrograms();
    ImmutableList<QuestionDefinition> questionDefinitions =
        questionService.getReadOnlyQuestionService().toCompletableFuture().join().getAllQuestions();
    return ok(
        view.render(
            request, activeAndDraftPrograms, questionDefinitions, request.flash().get("success")));
  }

  public Result seedCanonicalQuestions() {
    if (!isDevOrStaging) {
      return notFound();
    }

    databaseSeedTask.run();

    return redirect(routes.DatabaseSeedController.index().url())
        .flashing("success", "Canonical questions seeded");
  }

  public Result seed() {
    // TODO: Check whether test program already exists to prevent error.
    if (!isDevOrStaging) {
      return notFound();
    }
    QuestionDefinition canonicalNameQuestion =
        databaseSeedTask.run().stream()
            .filter(q -> q.getName().equals("Name"))
            .findFirst()
            .orElseThrow();
    insertProgramWithBlocks("mock-program", "Mock program", canonicalNameQuestion);
    insertSimpleProgram("simple-program", "Simple program", canonicalNameQuestion);
    return redirect(routes.DatabaseSeedController.index().url())
        .flashing("success", "The database has been seeded");
  }

  /** Remove all content from the program and question tables. */
  public Result clear() {
    if (!isDevOrStaging) {
      return notFound();
    }
    resetTables();
    return redirect(routes.DatabaseSeedController.index().url())
        .flashing("success", "The database has been cleared");
  }

  private QuestionDefinition insertAddressQuestionDefinition() {
    return questionService.create(ADDRESS_QUESTION_DEFINITION).getResult();
  }

  private QuestionDefinition insertCheckboxQuestionDefinition() {
    return questionService.create(CHECKBOX_QUESTION_DEFINITION).getResult();
  }

  private QuestionDefinition insertCurrencyQuestionDefinition() {
    return questionService.create(CURRENCY_QUESTION_DEFINITION).getResult();
  }

  private QuestionDefinition insertDateQuestionDefinitionForEnumerator(long enumeratorId) {
    return questionService.create(DATE_QUESTION_DEFINITION(enumeratorId)).getResult();
  }

  // Create a date question definition with the given name and questionText. We currently create
  // multiple date questions in a single program for testing.
  private QuestionDefinition insertDateQuestionDefinition(String name, String questionText) {
    return questionService.create(DATE_QUESTION_DEFINITION(name, questionText)).getResult();
  }

  private QuestionDefinition insertDropdownQuestionDefinition() {
    return questionService.create(DROPDOWN_QUESTION_DEFINITION).getResult();
  }

  private QuestionDefinition insertEmailQuestionDefinition() {
    return questionService.create(EMAIL_QUESTION_DEFINITION).getResult();
  }

  private QuestionDefinition insertEnumeratorQuestionDefinition() {
    return questionService.create(ENUMERATOR_QUESTION_DEFINITION).getResult();
  }

  private QuestionDefinition insertFileUploadQuestionDefinition() {
    return questionService.create(FILE_UPLOAD_QUESTION_DEFINITION).getResult();
  }

  private QuestionDefinition insertIdQuestionDefinition() {
    return questionService.create(ID_QUESTION_DEFINITION).getResult();
  }

  private QuestionDefinition insertNumberQuestionDefinition() {
    return questionService.create(NUMBER_QUESTION_DEFINITION).getResult();
  }

  private QuestionDefinition insertRadioButtonQuestionDefinition() {
    return questionService.create(RADIO_BUTTON_QUESTION_DEFINITION).getResult();
  }

  private QuestionDefinition insertStaticTextQuestionDefinition() {
    return questionService.create(STATIC_CONTENT_QUESTION_DEFINITION).getResult();
  }

  private QuestionDefinition insertTextQuestionDefinition() {
    return questionService.create(TEXT_QUESTION_DEFINITION).getResult();
  }

  private QuestionDefinition insertPhoneQuestionDefinition() {
    return questionService.create(PHONE_QUESTION_DEFINITION).getResult();
  }

  private ProgramDefinition insertProgramWithBlocks(
      String adminName, String displayName, QuestionDefinition nameQuestion) {
    try {
      ErrorAnd<ProgramDefinition, CiviFormError> programDefinitionResult =
          programService.createProgramDefinition(
              adminName,
              "desc",
              displayName,
              "display description",
              /* defaultConfirmationMessage= */ "",
              "https://github.com/seattle-uat/civiform",
              DisplayMode.PUBLIC.getValue(),
              /* programType= */ ProgramType.DEFAULT,
              /* isIntakeFormFeatureEnabled= */ false,
              ImmutableList.copyOf(new ArrayList<>()));
      if (programDefinitionResult.isError()) {
        throw new Exception(programDefinitionResult.getErrors().toString());
      }
      ProgramDefinition programDefinition = programDefinitionResult.getResult();
      long programId = programDefinition.id();

      long blockId = 1L;
      BlockForm blockForm = new BlockForm();
      blockForm.setName("Block 1");
      blockForm.setDescription("one of each question type - part 1");
      programService.updateBlock(programId, blockId, blockForm).getResult();
      programService.addQuestionsToBlock(
          programId,
          blockId,
          ImmutableList.of(
              insertStaticTextQuestionDefinition().getId(),
              insertAddressQuestionDefinition().getId(),
              insertCheckboxQuestionDefinition().getId(),
              insertCurrencyQuestionDefinition().getId(),
              insertDateQuestionDefinition("date", "When is your birthday?").getId(),
              insertDropdownQuestionDefinition().getId(),
              insertPhoneQuestionDefinition().getId()));

      blockId =
          programService.addBlockToProgram(programId).getResult().maybeAddedBlock().get().id();
      blockForm.setName("Block 2");
      blockForm.setDescription("one of each question type - part 2");
      programService.updateBlock(programId, blockId, blockForm);
      programService.addQuestionsToBlock(
          programId,
          blockId,
          ImmutableList.of(
              insertEmailQuestionDefinition().getId(),
              insertIdQuestionDefinition().getId(),
              nameQuestion.getId(),
              insertNumberQuestionDefinition().getId(),
              insertTextQuestionDefinition().getId()));

      blockId =
          programService.addBlockToProgram(programId).getResult().maybeAddedBlock().get().id();
      blockForm.setName("enumerator");
      blockForm.setDescription("this is for an enumerator");
      programService.updateBlock(programId, blockId, blockForm);
      long enumeratorId = insertEnumeratorQuestionDefinition().getId();
      programService.addQuestionsToBlock(programId, blockId, ImmutableList.of(enumeratorId));
      // Create repeated screens based on enumerator.
      long enumeratorBlockId = blockId;
      blockId =
          programService
              .addRepeatedBlockToProgram(programId, enumeratorBlockId)
              .getResult()
              .maybeAddedBlock()
              .get()
              .id();
      blockForm.setName("repeated screen for enumerator");
      blockForm.setDescription("this is a repeated screen for an enumerator");
      programService.updateBlock(programId, blockId, blockForm);
      programService.addQuestionsToBlock(
          programId,
          blockId,
          ImmutableList.of(insertDateQuestionDefinitionForEnumerator(enumeratorId).getId()));

      blockId =
          programService.addBlockToProgram(programId).getResult().maybeAddedBlock().get().id();
      blockForm.setName("Block 3");
      blockForm.setDescription("Random information");
      programService.updateBlock(programId, blockId, blockForm);
      long radioButtonQuestionId = insertRadioButtonQuestionDefinition().getId();
      programService.addQuestionsToBlock(
          programId, blockId, ImmutableList.of(radioButtonQuestionId));

      blockId =
          programService.addBlockToProgram(programId).getResult().maybeAddedBlock().get().id();
      blockForm.setName("Block with Predicate");
      blockForm.setDescription("May be hidden");
      programService.updateBlock(programId, blockId, blockForm);
      // Add an unanswered question to the block so it is considered incomplete.
      programService.addQuestionsToBlock(
          programId,
          blockId,
          ImmutableList.of(
              insertDateQuestionDefinition("predicate date", "When is your birthday?").getId()));
      // Add a predicate based on the "favorite season" radio button question in Block 3
      LeafOperationExpressionNode operation =
          LeafOperationExpressionNode.create(
              radioButtonQuestionId,
              Scalar.SELECTION,
              Operator.IN,
              PredicateValue.listOfStrings(ImmutableList.of("2", "3")));
      PredicateDefinition predicate =
          PredicateDefinition.create(
              PredicateExpressionNode.create(operation), PredicateAction.SHOW_BLOCK);
      programDefinition =
          programService.setBlockVisibilityPredicate(programId, blockId, Optional.of(predicate));

      // Add file upload as optional to make local testing easier.
      blockId =
          programService.addBlockToProgram(programId).getResult().maybeAddedBlock().get().id();
      blockForm.setName("file upload");
      blockForm.setDescription("this is for file upload");
      programService.updateBlock(programId, blockId, blockForm);
      long fileQuestionId = insertFileUploadQuestionDefinition().getId();
      programService.addQuestionsToBlock(programId, blockId, ImmutableList.of(fileQuestionId));
      programService.setProgramQuestionDefinitionOptionality(
          programId, blockId, fileQuestionId, true);

      return programDefinition;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private ProgramDefinition insertSimpleProgram(
      String adminName, String displayName, QuestionDefinition nameQuestion) {
    try {
      ErrorAnd<ProgramDefinition, CiviFormError> programDefinitionResult =
          programService.createProgramDefinition(
              adminName,
              "desc",
              displayName,
              "display description",
              /* defaultConfirmationMessage= */ "",
              /* externalLink= */ "https://github.com/seattle-uat/civiform",
              DisplayMode.PUBLIC.getValue(),
              /* programType= */ ProgramType.DEFAULT,
              /* isIntakeFormFeatureEnabled= */ false,
              ImmutableList.copyOf(new ArrayList<>()));
      if (programDefinitionResult.isError()) {
        throw new Exception(programDefinitionResult.getErrors().toString());
      }
      ProgramDefinition programDefinition = programDefinitionResult.getResult();
      long programId = programDefinition.id();

      long blockId = 1L;
      BlockForm blockForm = new BlockForm();
      blockForm.setName("Block 1");
      blockForm.setDescription("Block 1");
      programService.updateBlock(programId, blockId, blockForm).getResult();

      programService.addQuestionsToBlock(
          programId, blockId, ImmutableList.of(nameQuestion.getId()));
      programService.setProgramQuestionDefinitionOptionality(
          programId, blockId, nameQuestion.getId(), true);

      return programDefinition;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void resetTables() {
    Models.truncate(database);
    Version newActiveVersion = new Version(LifecycleStage.ACTIVE);
    newActiveVersion.save();
    settingsService.migrateConfigValuesToSettingsGroup();
  }
}
