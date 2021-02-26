package controllers.dev;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import io.ebean.Ebean;
import io.ebean.EbeanServer;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import models.Program;
import models.Question;
import play.db.ebean.EbeanConfig;
import play.inject.ApplicationLifecycle;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;
import services.program.ProgramDefinition;
import services.program.ProgramQuestionDefinition;
import services.program.ProgramService;
import services.question.AddressQuestionDefinition;
import services.question.NameQuestionDefinition;
import services.question.QuestionDefinition;
import services.question.QuestionService;
import services.question.TextQuestionDefinition;
import views.dev.DatabaseSeedView;

/** Controller for seeding the database with test content to develop against. */
public class DatabaseSeedController extends Controller {
  private final DatabaseSeedView view;
  private final ApplicationLifecycle appLifecycle;
  private final EbeanServer ebeanServer;
  private final QuestionService questionService;
  private final ProgramService programService;

  @Inject
  public DatabaseSeedController(
      DatabaseSeedView view,
      ApplicationLifecycle appLifecycle,
      EbeanConfig ebeanConfig,
      QuestionService questionService,
      ProgramService programService) {
    this.view = view;
    this.appLifecycle = appLifecycle;
    this.ebeanServer = Ebean.getServer(ebeanConfig.defaultServer());
    this.questionService = questionService;
    this.programService = programService;

    this.appLifecycle.addStopHook(
        () -> {
          truncateTables();
          return CompletableFuture.completedFuture(null);
        });
  }

  public Result index(Request request) {
    // Display state of the database in roughly formatted string
    // Offer two buttons:
    //   One to generate base mock content
    //   One to clear the databases
    ImmutableList<ProgramDefinition> programDefinitions = programService.listProgramDefinitions();
    ImmutableList<QuestionDefinition> questionDefinitions = questionService.getReadOnlyQuestionService().toCompletableFuture().join().getAllQuestions();
    return ok(view.render(request, programDefinitions, questionDefinitions, request.flash().get("success")));
  }

  public Result seed() {
    // Need to make sure to only insert the base mock content if it's not already there - otherwise
    // we will run into issues with duplicate paths. Do this by checking for the program rather
    // than truncating tables.
    truncateTables();
    insertProgramWithBlocks("Mock program");
    return redirect(routes.DatabaseSeedController.index().url())
        .flashing("success", "The database has been seeded");
  }

  public Result clear() {
    // Remove all content from database.
    truncateTables();
    return redirect(routes.DatabaseSeedController.index().url())
        .flashing("success", "The database has been cleared");
  }

  public QuestionDefinition insertNameQuestionDefinition() {
    return questionService
        .create(
            new NameQuestionDefinition(
                1L,
                "name",
                "applicant.name",
                "description",
                ImmutableMap.of(Locale.ENGLISH, "What is your name?"),
                ImmutableMap.of(Locale.ENGLISH, "help text")))
        .get();
  }

  public QuestionDefinition insertTextQuestionDefinition(String name) {
    return questionService
        .create(
            new TextQuestionDefinition(
                1L,
                name,
                "applicant." + name,
                "description",
                ImmutableMap.of(Locale.ENGLISH, "What is your favorite color?"),
                ImmutableMap.of(Locale.ENGLISH, "help text")))
        .get();
  }

  public QuestionDefinition insertAddressQuestionDefinition() {
    return questionService
        .create(
            new AddressQuestionDefinition(
                1L,
                "address",
                "applicant.address",
                "description",
                ImmutableMap.of(Locale.ENGLISH, "What is your address?"),
                ImmutableMap.of(Locale.ENGLISH, "help text")))
        .get();
  }

  public ProgramDefinition insertProgramWithBlocks(String name) {
    try {
      ProgramDefinition programDefinition = programService.createProgramDefinition(name, "desc");

      programDefinition =
          programService.addBlockToProgram(
              programDefinition.id(), "Block 1", "name and favorite color");
      programDefinition =
          programService.setBlockQuestions(
              programDefinition.id(),
              programDefinition.blockDefinitions().get(0).id(),
              ImmutableList.of(
                  ProgramQuestionDefinition.create(insertNameQuestionDefinition()),
                  ProgramQuestionDefinition.create(insertTextQuestionDefinition("color"))));

      programDefinition =
              programService.addBlockToProgram(
                      programDefinition.id(), "Block 2", "address");
      programDefinition =
              programService.setBlockQuestions(
                      programDefinition.id(),
                      programDefinition.blockDefinitions().get(1).id(),
                      ImmutableList.of(
                              ProgramQuestionDefinition.create(insertAddressQuestionDefinition())));

      return programDefinition;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void truncateTables() {
    ebeanServer.truncate(Program.class, Question.class);
  }
}
