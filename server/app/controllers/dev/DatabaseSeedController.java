package controllers.dev;

import static com.google.common.base.Preconditions.checkNotNull;

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
import services.LocalizedStrings;
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
import services.question.QuestionOption;
import services.question.QuestionService;
import services.question.types.AddressQuestionDefinition;
import services.question.types.CurrencyQuestionDefinition;
import services.question.types.DateQuestionDefinition;
import services.question.types.EmailQuestionDefinition;
import services.question.types.EnumeratorQuestionDefinition;
import services.question.types.FileUploadQuestionDefinition;
import services.question.types.IdQuestionDefinition;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.MultiOptionQuestionDefinition.MultiOptionQuestionType;
import services.question.types.NumberQuestionDefinition;
import services.question.types.PhoneQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionConfig;
import services.question.types.StaticContentQuestionDefinition;
import services.question.types.TextQuestionDefinition;
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
    return questionService
        .create(
            new AddressQuestionDefinition(
                QuestionDefinitionConfig.builder()
                    .setName("address")
                    .setDescription("description")
                    .setQuestionText(LocalizedStrings.withDefaultValue("What is your address?"))
                    .setQuestionHelpText(LocalizedStrings.withDefaultValue("help text"))
                    .build()))
        .getResult();
  }

  private QuestionDefinition insertCheckboxQuestionDefinition() {
    QuestionDefinitionConfig config =
        QuestionDefinitionConfig.builder()
            .setName("checkbox")
            .setDescription("description")
            .setQuestionText(
                LocalizedStrings.withDefaultValue(
                    "Which of the following kitchen instruments do you own?"))
            .setQuestionHelpText(LocalizedStrings.withDefaultValue("help text"))
            .build();
    ImmutableList<QuestionOption> questionOptions =
        ImmutableList.of(
            QuestionOption.create(1L, 1L, LocalizedStrings.withDefaultValue("toaster")),
            QuestionOption.create(2L, 2L, LocalizedStrings.withDefaultValue("pepper grinder")),
            QuestionOption.create(3L, 3L, LocalizedStrings.withDefaultValue("garlic press")));
    return questionService
        .create(
            new MultiOptionQuestionDefinition(
                config, questionOptions, MultiOptionQuestionType.CHECKBOX))
        .getResult();
  }

  private QuestionDefinition insertCurrencyQuestionDefinition() {
    return questionService
        .create(
            new CurrencyQuestionDefinition(
                QuestionDefinitionConfig.builder()
                    .setName("currency")
                    .setDescription("description")
                    .setQuestionText(
                        LocalizedStrings.withDefaultValue(
                            "How much should a scoop of ice cream cost?"))
                    .setQuestionHelpText(LocalizedStrings.withDefaultValue("help text"))
                    .build()))
        .getResult();
  }

  private QuestionDefinition insertDateQuestionDefinitionForEnumerator(long enumeratorId) {
    return questionService
        .create(
            new DateQuestionDefinition(
                QuestionDefinitionConfig.builder()
                    .setName("enumerator date")
                    .setDescription("description")
                    .setQuestionText(LocalizedStrings.withDefaultValue("When is $this's birthday?"))
                    .setQuestionHelpText(
                        LocalizedStrings.withDefaultValue("help text for $this's birthday"))
                    .setEnumeratorId(Optional.of(enumeratorId))
                    .build()))
        .getResult();
  }

  // Create a date question definition with the given name and questionText. We currently create
  // multiple date questions in a single program for testing.
  private QuestionDefinition insertDateQuestionDefinition(String name, String questionText) {
    return questionService
        .create(
            new DateQuestionDefinition(
                QuestionDefinitionConfig.builder()
                    .setName(name)
                    .setDescription("description")
                    .setQuestionText(LocalizedStrings.withDefaultValue(questionText))
                    .setQuestionHelpText(LocalizedStrings.withDefaultValue("help text"))
                    .build()))
        .getResult();
  }

  private QuestionDefinition insertDropdownQuestionDefinition() {
    QuestionDefinitionConfig config =
        QuestionDefinitionConfig.builder()
            .setName("dropdown")
            .setDescription("select your favorite ice cream flavor")
            .setQuestionText(
                LocalizedStrings.withDefaultValue(
                    "Select your favorite ice cream flavor from the following"))
            .setQuestionHelpText(LocalizedStrings.withDefaultValue("this is sample help text"))
            .build();
    ImmutableList<QuestionOption> questionOptions =
        ImmutableList.of(
            QuestionOption.create(1L, 1L, LocalizedStrings.withDefaultValue("chocolate")),
            QuestionOption.create(2L, 2L, LocalizedStrings.withDefaultValue("strawberry")),
            QuestionOption.create(3L, 3L, LocalizedStrings.withDefaultValue("vanilla")),
            QuestionOption.create(4L, 4L, LocalizedStrings.withDefaultValue("coffee")));
    return questionService
        .create(
            new MultiOptionQuestionDefinition(
                config, questionOptions, MultiOptionQuestionType.DROPDOWN))
        .getResult();
  }

  private QuestionDefinition insertEmailQuestionDefinition() {
    return questionService
        .create(
            new EmailQuestionDefinition(
                QuestionDefinitionConfig.builder()
                    .setName("email")
                    .setDescription("description")
                    .setQuestionText(LocalizedStrings.withDefaultValue("What is your email?"))
                    .setQuestionHelpText(LocalizedStrings.withDefaultValue("help text"))
                    .build()))
        .getResult();
  }

  private QuestionDefinition insertEnumeratorQuestionDefinition() {
    return questionService
        .create(
            new EnumeratorQuestionDefinition(
                QuestionDefinitionConfig.builder()
                    .setName("enumerator")
                    .setDescription("description")
                    .setQuestionText(
                        LocalizedStrings.withDefaultValue("List all members of your household."))
                    .setQuestionHelpText(LocalizedStrings.withDefaultValue("help text"))
                    .build(),
                LocalizedStrings.withDefaultValue("household member")))
        .getResult();
  }

  private QuestionDefinition insertFileUploadQuestionDefinition() {
    return questionService
        .create(
            new FileUploadQuestionDefinition(
                QuestionDefinitionConfig.builder()
                    .setName("file upload")
                    .setDescription("description")
                    .setQuestionText(
                        LocalizedStrings.withDefaultValue("Upload anything from your computer"))
                    .setQuestionHelpText(LocalizedStrings.withDefaultValue("help text"))
                    .build()))
        .getResult();
  }

  private QuestionDefinition insertIdQuestionDefinition() {
    return questionService
        .create(
            new IdQuestionDefinition(
                QuestionDefinitionConfig.builder()
                    .setName("id")
                    .setDescription("description")
                    .setQuestionText(
                        LocalizedStrings.withDefaultValue("What is your driver's license ID?"))
                    .setQuestionHelpText(LocalizedStrings.withDefaultValue("help text"))
                    .build()))
        .getResult();
  }

  private QuestionDefinition insertNumberQuestionDefinition() {
    return questionService
        .create(
            new NumberQuestionDefinition(
                QuestionDefinitionConfig.builder()
                    .setName("number")
                    .setDescription("description")
                    .setQuestionText(
                        LocalizedStrings.withDefaultValue("How many pets do you have?"))
                    .setQuestionHelpText(LocalizedStrings.withDefaultValue("help text"))
                    .build()))
        .getResult();
  }

  private QuestionDefinition insertRadioButtonQuestionDefinition() {
    QuestionDefinitionConfig config =
        QuestionDefinitionConfig.builder()
            .setName("radio")
            .setDescription("favorite season in the year")
            .setQuestionText(LocalizedStrings.withDefaultValue("What is your favorite season?"))
            .setQuestionHelpText(LocalizedStrings.withDefaultValue("this is sample help text"))
            .build();

    ImmutableList<QuestionOption> questionOptions =
        ImmutableList.of(
            QuestionOption.create(
                1L, 1L, LocalizedStrings.withDefaultValue("winter (will hide next block)")),
            QuestionOption.create(2L, 2L, LocalizedStrings.withDefaultValue("spring")),
            QuestionOption.create(3L, 3L, LocalizedStrings.withDefaultValue("summer")),
            QuestionOption.create(
                4L, 4L, LocalizedStrings.withDefaultValue("fall (will hide next block)")));
    return questionService
        .create(
            new MultiOptionQuestionDefinition(
                config, questionOptions, MultiOptionQuestionType.RADIO_BUTTON))
        .getResult();
  }

  private QuestionDefinition insertStaticTextQuestionDefinition() {
    return questionService
        .create(
            new StaticContentQuestionDefinition(
                QuestionDefinitionConfig.builder()
                    .setName("static content")
                    .setDescription("description")
                    .setQuestionText(
                        LocalizedStrings.withDefaultValue(
                            "Hi I'm a block of static text. \n"
                                + " * Welcome to this test program.\n"
                                + " * It contains one of every question type. \n\n"
                                + "### What are the eligibility requirements? \n"
                                + ">You are 18 years or older."))
                    .setQuestionHelpText(LocalizedStrings.withDefaultValue(""))
                    .build()))
        .getResult();
  }

  private QuestionDefinition insertTextQuestionDefinition() {
    return questionService
        .create(
            new TextQuestionDefinition(
                QuestionDefinitionConfig.builder()
                    .setName("text")
                    .setDescription("description")
                    .setQuestionText(
                        LocalizedStrings.withDefaultValue("What is your favorite color?"))
                    .setQuestionHelpText(LocalizedStrings.withDefaultValue("help text"))
                    .build()))
        .getResult();
  }

  private QuestionDefinition insertPhoneQuestionDefinition() {
    return questionService
        .create(
            new PhoneQuestionDefinition(
                QuestionDefinitionConfig.builder()
                    .setName("phone")
                    .setDescription("description")
                    .setQuestionText(LocalizedStrings.withDefaultValue("what is your phone number"))
                    .setQuestionHelpText(LocalizedStrings.withDefaultValue("help text"))
                    .build()))
        .getResult();
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
