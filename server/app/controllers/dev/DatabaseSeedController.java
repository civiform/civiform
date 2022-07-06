package controllers.dev;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import forms.BlockForm;
import io.ebean.DB;
import io.ebean.Database;
import java.util.Locale;
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
import services.program.ProgramQuestionDefinition;
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
import services.question.types.DropdownQuestionDefinition;
import services.question.types.NameQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.RadioButtonQuestionDefinition;
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
    // TODO: consider checking whether the test program already exists.
    if (!isDevOrStagingEnvironment()) {
      return notFound();
    }
    databaseSeedTask.run();
    insertProgramWithBlocks("Mock program");
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

  private QuestionDefinition insertNameQuestionDefinition() {
    // TODO(#2843): Remove this in favor of the question definition
    // created by the seed canonical questions task. This doesn't currently
    // conflict with the canonical question since it has a different
    // character casing. When constructing the mock program above, the
    // canonical name question will need to be retrieved.
    return questionService
        .create(
            new NameQuestionDefinition(
                "name",
                Optional.empty(),
                "description",
                LocalizedStrings.of(
                    Locale.US,
                    "What is your name?",
                    Locale.forLanguageTag("es-US"),
                    "¿Cómo se llama?"),
                LocalizedStrings.of(
                    Locale.US,
                    "help text",
                    Locale.forLanguageTag("es-US"),
                    "Ponga su nombre legal")))
        .getResult();
  }

  private QuestionDefinition insertColorQuestionDefinition() {
    return questionService
        .create(
            new TextQuestionDefinition(
                "color",
                Optional.empty(),
                "description",
                LocalizedStrings.of(Locale.US, "What is your favorite color?"),
                LocalizedStrings.of(Locale.US, "help text")))
        .getResult();
  }

  private QuestionDefinition insertAddressQuestionDefinition() {
    return questionService
        .create(
            new AddressQuestionDefinition(
                "address",
                Optional.empty(),
                "description",
                LocalizedStrings.of(Locale.US, "What is your address?"),
                LocalizedStrings.of(Locale.US, "help text")))
        .getResult();
  }

  private QuestionDefinition insertCheckboxQuestionDefinition() {
    return questionService
        .create(
            new CheckboxQuestionDefinition(
                "kitchen",
                Optional.empty(),
                "description",
                LocalizedStrings.of(
                    Locale.US, "Which of the following kitchen instruments do you own?"),
                LocalizedStrings.of(Locale.US, "help text"),
                ImmutableList.of(
                    QuestionOption.create(1L, 1L, LocalizedStrings.of(Locale.US, "toaster")),
                    QuestionOption.create(2L, 2L, LocalizedStrings.of(Locale.US, "pepper grinder")),
                    QuestionOption.create(3L, 3L, LocalizedStrings.of(Locale.US, "garlic press")))))
        .getResult();
  }

  private QuestionDefinition insertDropdownQuestionDefinition() {
    return questionService
        .create(
            new DropdownQuestionDefinition(
                "dropdown",
                Optional.empty(),
                "select your favorite ice cream flavor",
                LocalizedStrings.of(
                    Locale.US, "Select your favorite ice cream flavor from the following"),
                LocalizedStrings.of(Locale.US, "this is sample help text"),
                ImmutableList.of(
                    QuestionOption.create(1L, 1L, LocalizedStrings.of(Locale.US, "chocolate")),
                    QuestionOption.create(2L, 2L, LocalizedStrings.of(Locale.US, "strawberry")),
                    QuestionOption.create(3L, 3L, LocalizedStrings.of(Locale.US, "vanilla")),
                    QuestionOption.create(4L, 4L, LocalizedStrings.of(Locale.US, "coffee")))))
        .getResult();
  }

  private QuestionDefinition insertRadioButtonQuestionDefinition() {
    return questionService
        .create(
            new RadioButtonQuestionDefinition(
                "radio",
                Optional.empty(),
                "favorite season in the year",
                LocalizedStrings.of(Locale.US, "What is your favorite season?"),
                LocalizedStrings.of(Locale.US, "this is sample help text"),
                ImmutableList.of(
                    QuestionOption.create(
                        1L, 1L, LocalizedStrings.of(Locale.US, "winter (will hide next block)")),
                    QuestionOption.create(2L, 2L, LocalizedStrings.of(Locale.US, "spring")),
                    QuestionOption.create(3L, 3L, LocalizedStrings.of(Locale.US, "summer")),
                    QuestionOption.create(
                        4L, 4L, LocalizedStrings.of(Locale.US, "fall (will hide next block)")))))
        .getResult();
  }

  private ProgramDefinition insertProgramWithBlocks(String name) {
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
      blockForm.setDescription("name and favorite color");
      programService.updateBlock(programId, blockId, blockForm).getResult();
      programService.setBlockQuestions(
          programId,
          blockId,
          ImmutableList.of(
              ProgramQuestionDefinition.create(
                  insertNameQuestionDefinition(), Optional.of(programId)),
              ProgramQuestionDefinition.create(
                  insertColorQuestionDefinition(), Optional.of(programId))));

      blockId =
          programService.addBlockToProgram(programId).getResult().maybeAddedBlock().get().id();
      blockForm.setName("Block 2");
      blockForm.setDescription("address");
      programService.updateBlock(programId, blockId, blockForm);
      programService.addQuestionsToBlock(
          programId, blockId, ImmutableList.of(insertAddressQuestionDefinition().getId()));

      blockId =
          programService.addBlockToProgram(programId).getResult().maybeAddedBlock().get().id();
      blockForm.setName("Block 3");
      blockForm.setDescription("Ice Cream Information");
      programService.updateBlock(programId, blockId, blockForm);
      programService.addQuestionsToBlock(
          programId, blockId, ImmutableList.of(insertDropdownQuestionDefinition().getId()));

      blockId =
          programService.addBlockToProgram(programId).getResult().maybeAddedBlock().get().id();
      blockForm.setName("Block 4");
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
          programId, blockId, ImmutableList.of(insertCheckboxQuestionDefinition().getId()));
      // Add a predicate based on the "favorite season" radio button question in Block 4
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
