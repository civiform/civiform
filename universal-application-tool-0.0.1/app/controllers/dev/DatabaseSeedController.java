package controllers.dev;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import forms.BlockForm;
import io.ebean.Ebean;
import io.ebean.EbeanServer;
import java.util.Locale;
import models.LifecycleStage;
import models.Program;
import models.Question;
import play.Environment;
import play.db.ebean.EbeanConfig;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;
import services.Path;
import services.program.ProgramDefinition;
import services.program.ProgramQuestionDefinition;
import services.program.ProgramService;
import services.question.AddressQuestionDefinition;
import services.question.CheckboxQuestionDefinition;
import services.question.DropdownQuestionDefinition;
import services.question.NameQuestionDefinition;
import services.question.QuestionDefinition;
import services.question.QuestionService;
import services.question.TextQuestionDefinition;
import views.dev.DatabaseSeedView;

/** Controller for seeding the database with test content to develop against. */
public class DatabaseSeedController extends Controller {
  private final DatabaseSeedView view;
  private final EbeanServer ebeanServer;
  private final QuestionService questionService;
  private final ProgramService programService;
  private final Environment environment;

  @Inject
  public DatabaseSeedController(
      DatabaseSeedView view,
      EbeanConfig ebeanConfig,
      QuestionService questionService,
      ProgramService programService,
      Environment environment) {
    this.view = checkNotNull(view);
    this.ebeanServer = Ebean.getServer(checkNotNull(ebeanConfig).defaultServer());
    this.questionService = checkNotNull(questionService);
    this.programService = checkNotNull(programService);
    this.environment = checkNotNull(environment);
  }

  /**
   * Display state of the database in roughly formatted string. Displays a button to generate mock
   * database content and another to clear the database.
   */
  public Result index(Request request) {
    if (environment.isDev()) {
      ImmutableList<ProgramDefinition> programDefinitions = programService.listProgramDefinitions();
      ImmutableList<QuestionDefinition> questionDefinitions =
          questionService
              .getReadOnlyQuestionService()
              .toCompletableFuture()
              .join()
              .getAllQuestions();
      return ok(
          view.render(
              request, programDefinitions, questionDefinitions, request.flash().get("success")));
    } else {
      return notFound();
    }
  }

  public Result seed() {
    // TODO: consider checking whether the test program already exists.
    if (environment.isDev()) {
      insertProgramWithBlocks("Mock program");
      return redirect(routes.DatabaseSeedController.index().url())
          .flashing("success", "The database has been seeded");
    } else {
      return notFound();
    }
  }

  /** Remove all content from the program and question tables. */
  public Result clear() {
    if (environment.isDev()) {
      truncateTables();
      return redirect(routes.DatabaseSeedController.index().url())
          .flashing("success", "The database has been cleared");
    } else {
      return notFound();
    }
  }

  private QuestionDefinition insertNameQuestionDefinition() {
    return questionService
        .create(
            new NameQuestionDefinition(
                1L,
                "name",
                Path.create("applicant.name"),
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
                "description",
                LifecycleStage.ACTIVE,
                ImmutableMap.of(Locale.US, "Which of the following kitchen instruments do you own?"),
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
    ebeanServer.truncate(Program.class, Question.class);
  }
}
