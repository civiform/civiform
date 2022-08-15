package controllers.dev;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import forms.BlockForm;
import io.ebean.DB;
import io.ebean.Database;
import java.util.Optional;
import models.DisplayMode;
import models.LifecycleStage;
import models.Models;
import models.Version;
import play.Environment;
import play.mvc.Http.Request;
import play.mvc.Result;
import services.LocalizedStrings;
import services.applicant.question.Scalar;
import services.program.ActiveAndDraftPrograms;
import services.program.ProgramDefinition;
import services.program.ProgramService;
import services.program.predicate.LeafOperationExpressionNode;
import services.program.predicate.Operator;
import services.program.predicate.PredicateAction;
import services.program.predicate.PredicateDefinition;
import services.program.predicate.PredicateExpressionNode;
import services.program.predicate.PredicateValue;
import services.question.QuestionOption;
import services.question.QuestionService;
import services.question.types.AddressQuestionDefinition;
import services.question.types.CheckboxQuestionDefinition;
import services.question.types.CurrencyQuestionDefinition;
import services.question.types.DateQuestionDefinition;
import services.question.types.DropdownQuestionDefinition;
import services.question.types.EmailQuestionDefinition;
import services.question.types.EnumeratorQuestionDefinition;
import services.question.types.FileUploadQuestionDefinition;
import services.question.types.IdQuestionDefinition;
import services.question.types.NumberQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.RadioButtonQuestionDefinition;
import services.question.types.StaticContentQuestionDefinition;
import services.question.types.TextQuestionDefinition;
import tasks.DatabaseSeedTask;
import views.dev.DatabaseSeedView;

/** Controller for seeding the database with test content to develop against. */
public class DatabaseSeedController extends DevController {

  private final DatabaseSeedTask databaseSeedTask;
  private final DatabaseSeedView view;
  private final Database database;
  private final QuestionService questionService;
  private final ProgramService programService;

  @Inject
  public DatabaseSeedController(
      DatabaseSeedTask databaseSeedTask,
      DatabaseSeedView view,
      QuestionService questionService,
      ProgramService programService,
      Environment environment,
      Config configuration) {
    super(environment, configuration);
    this.databaseSeedTask = checkNotNull(databaseSeedTask);
    this.view = checkNotNull(view);
    this.database = DB.getDefault();
    this.questionService = checkNotNull(questionService);
    this.programService = checkNotNull(programService);
  }

  /**
   * Display state of the database in roughly formatted string. Displays a button to generate mock
   * database content and another to clear the database.
   */
  public Result index(Request request) {
    if (!isDevOrStagingEnvironment()) {
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
    if (!isDevOrStagingEnvironment()) {
      return notFound();
    }

    databaseSeedTask.run();

    return redirect(routes.DatabaseSeedController.index().url())
        .flashing("success", "Canonical questions seeded");
  }

  public Result seed() {
    // TODO: Check whether test program already exists to prevent error.
    if (!isDevOrStagingEnvironment()) {
      return notFound();
    }
    QuestionDefinition canonicalNameQuestion =
        databaseSeedTask.run().stream()
            .filter(q -> q.getName().equals("Name"))
            .findFirst()
            .orElseThrow();
    insertProgramWithBlocks("Mock program", canonicalNameQuestion);
    return redirect(routes.DatabaseSeedController.index().url())
        .flashing("success", "The database has been seeded");
  }

  /** Remove all content from the program and question tables. */
  public Result clear() {
    if (!isDevOrStagingEnvironment()) {
      return notFound();
    }
    resetTables();
    return redirect(routes.DatabaseSeedController.index().url())
        .flashing("success", "The database has been cleared");
  }

  private QuestionDefinition insertAddressQuestionDefinition() {
    return questionService
        .create(
            new AddressQuestionDefinition(
                "address",
                Optional.empty(),
                "description",
                LocalizedStrings.withDefaultValue("What is your address?"),
                LocalizedStrings.withDefaultValue("help text")))
        .getResult();
  }

  private QuestionDefinition insertCheckboxQuestionDefinition() {
    return questionService
        .create(
            new CheckboxQuestionDefinition(
                "checkbox",
                Optional.empty(),
                "description",
                LocalizedStrings.withDefaultValue(
                    "Which of the following kitchen instruments do you own?"),
                LocalizedStrings.withDefaultValue("help text"),
                ImmutableList.of(
                    QuestionOption.create(1L, 1L, LocalizedStrings.withDefaultValue("toaster")),
                    QuestionOption.create(
                        2L, 2L, LocalizedStrings.withDefaultValue("pepper grinder")),
                    QuestionOption.create(
                        3L, 3L, LocalizedStrings.withDefaultValue("garlic press")))))
        .getResult();
  }

  private QuestionDefinition insertCurrencyQuestionDefinition() {
    return questionService
        .create(
            new CurrencyQuestionDefinition(
                "currency",
                Optional.empty(),
                "description",
                LocalizedStrings.withDefaultValue("How much should a scoop of ice cream cost?"),
                LocalizedStrings.withDefaultValue("help text")))
        .getResult();
  }

  private QuestionDefinition insertDateQuestionDefinitionForEnumerator(long enumeratorId) {
    return questionService
        .create(
            new DateQuestionDefinition(
                "enumerator date",
                Optional.of(enumeratorId),
                "description",
                LocalizedStrings.withDefaultValue("When is $this's birthday?"),
                LocalizedStrings.withDefaultValue("help text for $this's birthday")))
        .getResult();
  }

  // Create a date question definition with the given name and questionText. We currently create
  // multiple date questions in a single program for testing.
  private QuestionDefinition insertDateQuestionDefinition(String name, String questionText) {
    return questionService
        .create(
            new DateQuestionDefinition(
                name,
                Optional.empty(),
                "description",
                LocalizedStrings.withDefaultValue(questionText),
                LocalizedStrings.withDefaultValue("help text")))
        .getResult();
  }

  private QuestionDefinition insertDropdownQuestionDefinition() {
    return questionService
        .create(
            new DropdownQuestionDefinition(
                "dropdown",
                Optional.empty(),
                "select your favorite ice cream flavor",
                LocalizedStrings.withDefaultValue(
                    "Select your favorite ice cream flavor from the following"),
                LocalizedStrings.withDefaultValue("this is sample help text"),
                ImmutableList.of(
                    QuestionOption.create(1L, 1L, LocalizedStrings.withDefaultValue("chocolate")),
                    QuestionOption.create(2L, 2L, LocalizedStrings.withDefaultValue("strawberry")),
                    QuestionOption.create(3L, 3L, LocalizedStrings.withDefaultValue("vanilla")),
                    QuestionOption.create(4L, 4L, LocalizedStrings.withDefaultValue("coffee")))))
        .getResult();
  }

  private QuestionDefinition insertEmailQuestionDefinition() {
    return questionService
        .create(
            new EmailQuestionDefinition(
                "email",
                Optional.empty(),
                "description",
                LocalizedStrings.withDefaultValue("What is your email?"),
                LocalizedStrings.withDefaultValue("help text")))
        .getResult();
  }

  private QuestionDefinition insertEnumeratorQuestionDefinition() {
    return questionService
        .create(
            new EnumeratorQuestionDefinition(
                "enumerator",
                Optional.empty(),
                "description",
                LocalizedStrings.withDefaultValue("List all members of your household."),
                LocalizedStrings.withDefaultValue("help text"),
                LocalizedStrings.withDefaultValue("household member")))
        .getResult();
  }

  private QuestionDefinition insertFileUploadQuestionDefinition() {
    return questionService
        .create(
            new FileUploadQuestionDefinition(
                "file upload",
                Optional.empty(),
                "description",
                LocalizedStrings.withDefaultValue("Upload anything from your computer"),
                LocalizedStrings.withDefaultValue("help text")))
        .getResult();
  }

  private QuestionDefinition insertIdQuestionDefinition() {
    return questionService
        .create(
            new IdQuestionDefinition(
                "id",
                Optional.empty(),
                "description",
                LocalizedStrings.withDefaultValue("What is your driver's license ID?"),
                LocalizedStrings.withDefaultValue("help text")))
        .getResult();
  }

  private QuestionDefinition insertNumberQuestionDefinition() {
    return questionService
        .create(
            new NumberQuestionDefinition(
                "number",
                Optional.empty(),
                "description",
                LocalizedStrings.withDefaultValue("How many pets do you have?"),
                LocalizedStrings.withDefaultValue("help text")))
        .getResult();
  }

  private QuestionDefinition insertRadioButtonQuestionDefinition() {
    return questionService
        .create(
            new RadioButtonQuestionDefinition(
                "radio",
                Optional.empty(),
                "favorite season in the year",
                LocalizedStrings.withDefaultValue("What is your favorite season?"),
                LocalizedStrings.withDefaultValue("this is sample help text"),
                ImmutableList.of(
                    QuestionOption.create(
                        1L, 1L, LocalizedStrings.withDefaultValue("winter (will hide next block)")),
                    QuestionOption.create(2L, 2L, LocalizedStrings.withDefaultValue("spring")),
                    QuestionOption.create(3L, 3L, LocalizedStrings.withDefaultValue("summer")),
                    QuestionOption.create(
                        4L, 4L, LocalizedStrings.withDefaultValue("fall (will hide next block)")))))
        .getResult();
  }

  private QuestionDefinition insertStaticTextQuestionDefinition() {
    return questionService
        .create(
            new StaticContentQuestionDefinition(
                "static content",
                Optional.empty(),
                "description",
                LocalizedStrings.withDefaultValue(
                    "Hi I'm a block of static text. \n"
                        + " * Welcome to this test program.\n"
                        + " * It contains one of every question type. \n\n"
                        + "### What are the eligibility requirements? \n"
                        + ">You are 18 years or older."),
                LocalizedStrings.withDefaultValue("")))
        .getResult();
  }

  private QuestionDefinition insertTextQuestionDefinition() {
    return questionService
        .create(
            new TextQuestionDefinition(
                "text",
                Optional.empty(),
                "description",
                LocalizedStrings.withDefaultValue("What is your favorite color?"),
                LocalizedStrings.withDefaultValue("help text")))
        .getResult();
  }

  private ProgramDefinition insertProgramWithBlocks(String name, QuestionDefinition nameQuestion) {
    try {
      ProgramDefinition programDefinition =
          programService
              .createProgramDefinition(
                  name,
                  "desc",
                  name,
                  "display description",
                  "https://github.com/seattle-uat/civiform",
                  DisplayMode.PUBLIC.getValue())
              .getResult();
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
              insertDropdownQuestionDefinition().getId()));

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
      programDefinition = programService.setBlockPredicate(programId, blockId, predicate);

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

  private void resetTables() {
    Models.truncate(database);
    Version newActiveVersion = new Version(LifecycleStage.ACTIVE);
    newActiveVersion.save();
  }
}
