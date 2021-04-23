package controllers.dev;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
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
import services.question.QuestionOption;
import services.question.QuestionService;
import services.question.types.AddressQuestionDefinition;
import services.question.types.CheckboxQuestionDefinition;
import services.question.types.DropdownQuestionDefinition;
import services.question.types.NameQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.RadioButtonQuestionDefinition;
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
                "name",
                Path.create("applicant.name"),
                Optional.empty(),
                "description",
                ImmutableMap.of(
                    Locale.US,
                    "What is your name?",
                    Locale.forLanguageTag("es-US"),
                    "¿Cómo se llama?"),
                ImmutableMap.of(
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
                Path.create("applicant.color"),
                Optional.empty(),
                "description",
                ImmutableMap.of(Locale.US, "What is your favorite color?"),
                ImmutableMap.of(Locale.US, "help text")))
        .getResult();
  }

  private QuestionDefinition insertAddressQuestionDefinition() {
    return questionService
        .create(
            new AddressQuestionDefinition(
                "address",
                Path.create("applicant.address"),
                Optional.empty(),
                "description",
                ImmutableMap.of(Locale.US, "What is your address?"),
                ImmutableMap.of(Locale.US, "help text")))
        .getResult();
  }

  private QuestionDefinition insertCheckboxQuestionDefinition() {
    return questionService
        .create(
            new CheckboxQuestionDefinition(
                "kitchen",
                Path.create("applicant.kitchen"),
                Optional.empty(),
                "description",
                ImmutableMap.of(
                    Locale.US, "Which of the following kitchen instruments do you own?"),
                ImmutableMap.of(Locale.US, "help text"),
                ImmutableList.of(
                    QuestionOption.create(1L, ImmutableMap.of(Locale.US, "toaster")),
                    QuestionOption.create(2L, ImmutableMap.of(Locale.US, "pepper grinder")),
                    QuestionOption.create(3L, ImmutableMap.of(Locale.US, "garlic press")))))
        .getResult();
  }

  private QuestionDefinition insertDropdownQuestionDefinition() {
    return questionService
        .create(
            new DropdownQuestionDefinition(
                "dropdown",
                Path.create("applicant.dropdown"),
                Optional.empty(),
                "select your favorite ice cream flavor",
                ImmutableMap.of(
                    Locale.US, "Select your favorite ice cream flavor from the following"),
                ImmutableMap.of(Locale.US, "this is sample help text"),
                ImmutableList.of(
                    QuestionOption.create(1L, ImmutableMap.of(Locale.US, "chocolate")),
                    QuestionOption.create(2L, ImmutableMap.of(Locale.US, "strawberry")),
                    QuestionOption.create(3L, ImmutableMap.of(Locale.US, "vanilla")),
                    QuestionOption.create(4L, ImmutableMap.of(Locale.US, "coffee")))))
        .getResult();
  }

  private QuestionDefinition insertRadioButtonQuestionDefinition() {
    return questionService
        .create(
            new RadioButtonQuestionDefinition(
                "radio",
                Path.create("applicant.radio"),
                Optional.empty(),
                "favorite season in the year",
                ImmutableMap.of(Locale.US, "What is your favorite season?"),
                ImmutableMap.of(Locale.US, "this is sample help text"),
                ImmutableList.of(
                    QuestionOption.create(1L, ImmutableMap.of(Locale.US, "winter")),
                    QuestionOption.create(2L, ImmutableMap.of(Locale.US, "spring")),
                    QuestionOption.create(3L, ImmutableMap.of(Locale.US, "summer")),
                    QuestionOption.create(4L, ImmutableMap.of(Locale.US, "fall")))))
        .getResult();
  }

  private ProgramDefinition insertProgramWithBlocks(String name) {
    try {
      ProgramDefinition programDefinition =
          programService
              .createProgramDefinition(name, "desc", name, "display description")
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
              ProgramQuestionDefinition.create(insertNameQuestionDefinition()),
              ProgramQuestionDefinition.create(insertColorQuestionDefinition())));

      blockId =
          programService.addBlockToProgram(programId).getResult().getLastBlockDefinition().id();
      blockForm.setName("Block 2");
      blockForm.setDescription("address");
      programService.updateBlock(programId, blockId, blockForm);
      programService.addQuestionsToBlock(
          programId, blockId, ImmutableList.of(insertAddressQuestionDefinition().getId()));

      blockId =
          programService.addBlockToProgram(programId).getResult().getLastBlockDefinition().id();
      blockForm.setName("Block 3");
      blockForm.setDescription("Ice Cream Information");
      programService.updateBlock(programId, blockId, blockForm);
      programService.addQuestionsToBlock(
          programId, blockId, ImmutableList.of(insertDropdownQuestionDefinition().getId()));

      blockId =
          programService.addBlockToProgram(programId).getResult().getLastBlockDefinition().id();
      blockForm.setName("Block 4");
      blockForm.setDescription("Random information");
      programService.updateBlock(programId, blockId, blockForm);
      programDefinition =
          programService.addQuestionsToBlock(
              programId,
              blockId,
              ImmutableList.of(
                  insertCheckboxQuestionDefinition().getId(),
                  insertRadioButtonQuestionDefinition().getId()));

      return programDefinition;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void truncateTables() {
    ebeanServer.truncate(
        Program.class,
        Question.class,
        Account.class,
        Applicant.class,
        Application.class,
        models.Version.class,
        StoredFile.class);
  }
}
