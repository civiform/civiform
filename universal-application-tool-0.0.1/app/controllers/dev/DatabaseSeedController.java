package controllers.dev;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import forms.BlockForm;
import io.ebean.Ebean;
import io.ebean.EbeanServer;
import java.util.Locale;
import java.util.Optional;
import models.Account;
import models.Applicant;
import models.Application;
import models.LifecycleStage;
import models.Program;
import models.Question;
import models.StoredFile;
import play.Environment;
import play.db.ebean.EbeanConfig;
import play.mvc.Http.Request;
import play.mvc.Result;
import services.Path;
import services.program.ProgramDefinition;
import services.program.ProgramQuestionDefinition;
import services.program.ProgramService;
import services.question.QuestionService;
import services.question.types.AddressQuestionDefinition;
import services.question.types.CheckboxQuestionDefinition;
import services.question.types.DropdownQuestionDefinition;
import services.question.types.NameQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.TextQuestionDefinition;
import views.dev.DatabaseSeedView;

/** Controller for seeding the database with test content to develop against. */
public class DatabaseSeedController extends DevController {
  private final DatabaseSeedView view;
  private final EbeanServer ebeanServer;
  private final QuestionService questionService;
  private final ProgramService programService;

  @Inject
  public DatabaseSeedController(
      DatabaseSeedView view,
      EbeanConfig ebeanConfig,
      QuestionService questionService,
      ProgramService programService,
      Environment environment,
      Config configuration) {
    super(environment, configuration);
    this.view = checkNotNull(view);
    this.ebeanServer = Ebean.getServer(checkNotNull(ebeanConfig).defaultServer());
    this.questionService = checkNotNull(questionService);
    this.programService = checkNotNull(programService);
  }

  /**
   * Display state of the database in roughly formatted string. Displays a button to generate mock
   * database content and another to clear the database.
   */
  public Result index(Request request) {
    if (!isDevEnvironment()) {
      return notFound();
    }
    ImmutableList<ProgramDefinition> programDefinitions = programService.listProgramDefinitions();
    ImmutableList<QuestionDefinition> questionDefinitions =
        questionService.getReadOnlyQuestionService().toCompletableFuture().join().getAllQuestions();
    return ok(
        view.render(
            request, programDefinitions, questionDefinitions, request.flash().get("success")));
  }

  public Result seed() {
    // TODO: consider checking whether the test program already exists.
    if (!isDevEnvironment()) {
      return notFound();
    }
    insertProgramWithBlocks("Mock program");
    return redirect(routes.DatabaseSeedController.index().url())
        .flashing("success", "The database has been seeded");
  }

  /** Remove all content from the program and question tables. */
  public Result clear() {
    if (!isDevEnvironment()) {
      return notFound();
    }
    truncateTables();
    return redirect(routes.DatabaseSeedController.index().url())
        .flashing("success", "The database has been cleared");
  }

  private QuestionDefinition insertNameQuestionDefinition() {
    return questionService
        .create(
            new NameQuestionDefinition(
                1L,
                "name",
                Path.create("applicant.name"),
                Optional.empty(),
                "description",
                LifecycleStage.ACTIVE,
                ImmutableMap.of(Locale.US, "What is your name?"),
                ImmutableMap.of(Locale.US, "help text")))
        .getResult();
  }

  private QuestionDefinition insertColorQuestionDefinition() {
    return questionService
        .create(
            new TextQuestionDefinition(
                1L,
                "color",
                Path.create("applicant.color"),
                Optional.empty(),
                "description",
                LifecycleStage.ACTIVE,
                ImmutableMap.of(Locale.US, "What is your favorite color?"),
                ImmutableMap.of(Locale.US, "help text")))
        .getResult();
  }

  private QuestionDefinition insertAddressQuestionDefinition() {
    return questionService
        .create(
            new AddressQuestionDefinition(
                1L,
                "address",
                Path.create("applicant.address"),
                Optional.empty(),
                "description",
                LifecycleStage.ACTIVE,
                ImmutableMap.of(Locale.US, "What is your address?"),
                ImmutableMap.of(Locale.US, "help text")))
        .getResult();
  }

  private QuestionDefinition insertCheckboxQuestionDefinition() {
    return questionService
        .create(
            new CheckboxQuestionDefinition(
                1L,
                "kitchen",
                Path.create("applicant.kitchen"),
                Optional.empty(),
                "description",
                LifecycleStage.ACTIVE,
                ImmutableMap.of(
                    Locale.US, "Which of the following kitchen instruments do you own?"),
                ImmutableMap.of(Locale.US, "help text"),
                ImmutableListMultimap.of(
                    Locale.US, "toaster", Locale.US, "pepper grinder", Locale.US, "garlic press")))
        .getResult();
  }

  private QuestionDefinition insertDropdownQuestionDefinition() {
    return questionService
        .create(
            new DropdownQuestionDefinition(
                1L,
                "dropdown",
                Path.create("applicant.dropdown"),
                Optional.empty(),
                "select your favorite ice cream flavor",
                LifecycleStage.ACTIVE,
                ImmutableMap.of(
                    Locale.US, "Select your favorite ice cream flavor from the following"),
                ImmutableMap.of(Locale.US, "this is sample help text"),
                ImmutableListMultimap.of(
                    Locale.US,
                    "chocolate",
                    Locale.US,
                    "strawberry",
                    Locale.US,
                    "vanilla",
                    Locale.US,
                    "coffee")))
        .getResult();
  }

  private ProgramDefinition insertProgramWithBlocks(String name) {
    try {
      ProgramDefinition programDefinition =
          programService.createProgramDefinition(name, "desc").getResult();

      BlockForm firstBlockForm = new BlockForm();
      firstBlockForm.setName("Block 1");
      firstBlockForm.setDescription("name and favorite color");

      programDefinition =
          programService
              .updateBlock(
                  programDefinition.id(),
                  programDefinition.blockDefinitions().get(0).id(),
                  firstBlockForm)
              .getResult();
      programDefinition =
          programService.setBlockQuestions(
              programDefinition.id(),
              programDefinition.blockDefinitions().get(0).id(),
              ImmutableList.of(
                  ProgramQuestionDefinition.create(insertNameQuestionDefinition()),
                  ProgramQuestionDefinition.create(insertColorQuestionDefinition())));

      programDefinition =
          programService
              .addBlockToProgram(
                  programDefinition.id(),
                  "Block 2",
                  "address",
                  ImmutableList.of(
                      ProgramQuestionDefinition.create(insertAddressQuestionDefinition())))
              .getResult();

      programDefinition =
          programService
              .addBlockToProgram(
                  programDefinition.id(),
                  "Block 3",
                  "Ice Cream Information",
                  ImmutableList.of(
                      ProgramQuestionDefinition.create(insertDropdownQuestionDefinition())))
              .getResult();

      programDefinition =
          programService
              .addBlockToProgram(
                  programDefinition.id(),
                  "Block 4",
                  "kitchen information",
                  ImmutableList.of(
                      ProgramQuestionDefinition.create(insertCheckboxQuestionDefinition())))
              .getResult();

      return programDefinition;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void truncateTables() {
    ebeanServer.truncate(Program.class, Question.class, Account.class, Applicant.class, Application.class, StoredFile.class);
  }
}
