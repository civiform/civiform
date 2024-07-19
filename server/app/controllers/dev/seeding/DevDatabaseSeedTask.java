package controllers.dev.seeding;

import static com.google.common.base.Preconditions.checkNotNull;
import static controllers.dev.seeding.SampleQuestionDefinitions.ADDRESS_QUESTION_DEFINITION;
import static controllers.dev.seeding.SampleQuestionDefinitions.CHECKBOX_QUESTION_DEFINITION;
import static controllers.dev.seeding.SampleQuestionDefinitions.CURRENCY_QUESTION_DEFINITION;
import static controllers.dev.seeding.SampleQuestionDefinitions.DATE_PREDICATE_QUESTION_DEFINITION;
import static controllers.dev.seeding.SampleQuestionDefinitions.DATE_QUESTION_DEFINITION;
import static controllers.dev.seeding.SampleQuestionDefinitions.DROPDOWN_QUESTION_DEFINITION;
import static controllers.dev.seeding.SampleQuestionDefinitions.EMAIL_QUESTION_DEFINITION;
import static controllers.dev.seeding.SampleQuestionDefinitions.ENUMERATOR_QUESTION_DEFINITION;
import static controllers.dev.seeding.SampleQuestionDefinitions.FILE_UPLOAD_QUESTION_DEFINITION;
import static controllers.dev.seeding.SampleQuestionDefinitions.ID_QUESTION_DEFINITION;
import static controllers.dev.seeding.SampleQuestionDefinitions.NAME_QUESTION_DEFINITION;
import static controllers.dev.seeding.SampleQuestionDefinitions.NUMBER_QUESTION_DEFINITION;
import static controllers.dev.seeding.SampleQuestionDefinitions.PHONE_QUESTION_DEFINITION;
import static controllers.dev.seeding.SampleQuestionDefinitions.RADIO_BUTTON_QUESTION_DEFINITION;
import static controllers.dev.seeding.SampleQuestionDefinitions.STATIC_CONTENT_QUESTION_DEFINITION;
import static controllers.dev.seeding.SampleQuestionDefinitions.TEXT_QUESTION_DEFINITION;
import static controllers.dev.seeding.SampleQuestionDefinitions.dateEnumeratedQuestionDefinition;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import forms.BlockForm;
import io.ebean.DB;
import io.ebean.Database;
import io.ebean.SerializableConflictException;
import io.ebean.Transaction;
import io.ebean.TxScope;
import io.ebean.annotation.TxIsolation;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.persistence.NonUniqueResultException;
import javax.persistence.RollbackException;
import models.CategoryModel;
import models.DisplayMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Environment;
import repository.CategoryRepository;
import repository.VersionRepository;
import services.CiviFormError;
import services.ErrorAnd;
import services.LocalizedStrings;
import services.applicant.question.Scalar;
import services.program.CantAddQuestionToBlockException;
import services.program.IllegalPredicateOrderingException;
import services.program.ProgramBlockDefinitionNotFoundException;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramQuestionDefinitionNotFoundException;
import services.program.ProgramService;
import services.program.ProgramType;
import services.program.predicate.LeafOperationExpressionNode;
import services.program.predicate.Operator;
import services.program.predicate.PredicateAction;
import services.program.predicate.PredicateDefinition;
import services.program.predicate.PredicateExpressionNode;
import services.program.predicate.PredicateValue;
import services.question.QuestionService;
import services.question.exceptions.QuestionNotFoundException;
import services.question.types.QuestionDefinition;
import services.statuses.DuplicateStatusException;
import services.statuses.StatusDefinitions;
import services.statuses.StatusService;

/**
 * Task for seeding the development database for manual and automated testing. Seeding for prod
 * should take place in {@link services.seeding.DatabaseSeedTask}.
 *
 * <p>To avoid attempting to insert duplicate resources it uses a thread-local database transaction
 * with a transaction scope of SERIALIZABLE. If the transaction fails it retries up to {@code
 * MAX_RETRIES} times, sleeping the thread with exponential backoff + jitter before each retry.
 *
 * <p>If the task fails {@code MAX_RETRIES} times, it throws a {@code RuntimeException} wrapping the
 * exception causing it to fail. This will not cause the server itself to crash or prevent it from
 * starting and serving requests.
 */
public final class DevDatabaseSeedTask {

  private static final Logger LOGGER = LoggerFactory.getLogger(DevDatabaseSeedTask.class);
  private static final int MAX_RETRIES = 10;
  private final QuestionService questionService;
  private final ProgramService programService;
  private final StatusService statusService;
  private final Environment environment;
  private final VersionRepository versionRepository;
  private final CategoryRepository categoryRepository;
  private final Database database;

  @Inject
  public DevDatabaseSeedTask(
      QuestionService questionService,
      ProgramService programService,
      StatusService statusService,
      VersionRepository versionRepository,
      CategoryRepository categoryRepository,
      Environment environment) {
    this.questionService = checkNotNull(questionService);
    this.statusService = checkNotNull(statusService);
    this.versionRepository = checkNotNull(versionRepository);
    this.categoryRepository = checkNotNull(categoryRepository);
    this.programService = checkNotNull(programService);
    this.database = DB.getDefault();
    this.environment = checkNotNull(environment);
  }

  /**
   * Ensures that all questions in {@link SampleQuestionDefinitions} are present in the database,
   * inserting the definitions in if any aren't found.
   */
  public ImmutableList<QuestionDefinition> seedQuestions() {
    ImmutableList<QuestionDefinition> sampleQuestionDefinitions =
        SampleQuestionDefinitions.ALL_SAMPLE_QUESTION_DEFINITIONS;
    ImmutableSet<String> sampleQuestionNames =
        sampleQuestionDefinitions.stream()
            .map(QuestionDefinition::getName)
            .collect(ImmutableSet.toImmutableSet());
    ImmutableMap<String, QuestionDefinition> existingSampleQuestions =
        questionService.getExistingQuestions(sampleQuestionNames);
    if (existingSampleQuestions.size() < sampleQuestionNames.size()) {
      // Ensure a draft version exists to avoid transaction collisions with getDraftVersion.
      versionRepository.getDraftVersionOrCreate();
    }

    ImmutableList.Builder<QuestionDefinition> questionDefinitions = ImmutableList.builder();
    for (QuestionDefinition questionDefinition : sampleQuestionDefinitions) {
      if (existingSampleQuestions.containsKey(questionDefinition.getName())) {
        LOGGER.info("Sample question \"{}\" exists at server start", questionDefinition.getName());
        questionDefinitions.add(existingSampleQuestions.get(questionDefinition.getName()));
      } else {
        inSerializableTransaction(
            () -> {
              Optional<QuestionDefinition> question = createQuestion(questionDefinition);
              question.ifPresent(questionDefinitions::add);
            },
            1);
      }
    }
    return questionDefinitions.build();
  }

  private Optional<QuestionDefinition> createQuestion(QuestionDefinition questionDefinition) {
    ErrorAnd<QuestionDefinition, CiviFormError> result = questionService.create(questionDefinition);

    if (result.isError()) {
      String errorMessages =
          result.getErrors().stream().map(CiviFormError::message).collect(Collectors.joining(", "));

      LOGGER.error(
          String.format(
              "Unable to create sample question \"%s\" due to %s",
              questionDefinition.getName(), errorMessages));
      return Optional.empty();
    } else {
      LOGGER.info("Sample sample question \"{}\"", questionDefinition.getName());
      return Optional.of(result.getResult());
    }
  }

  public void insertMinimalSampleProgram(ImmutableList<QuestionDefinition> createdSampleQuestions) {
    try {
      ErrorAnd<ProgramDefinition, CiviFormError> programDefinitionResult =
          programService.createProgramDefinition(
              "minimal-sample-program",
              "desc",
              "Minimal Sample Program",
              "display description",
              /* defaultConfirmationMessage= */ "",
              /* externalLink= */ "https://github.com/civiform/civiform",
              DisplayMode.PUBLIC.getValue(),
              /* eligibilityIsGating= */ true,
              /* programType= */ ProgramType.DEFAULT,
              /* isIntakeFormFeatureEnabled= */ false,
              ImmutableList.copyOf(new ArrayList<>()));
      if (programDefinitionResult.isError()) {
        throw new RuntimeException(programDefinitionResult.getErrors().toString());
      }
      ProgramDefinition programDefinition = programDefinitionResult.getResult();
      long programId = programDefinition.id();

      long blockId = 1L;
      BlockForm blockForm = new BlockForm();
      blockForm.setName("Screen 1");
      blockForm.setDescription("Screen 1");
      programService.updateBlock(programId, blockId, blockForm).getResult();

      long nameQuestionId = getCreatedId(NAME_QUESTION_DEFINITION, createdSampleQuestions);
      programService.addQuestionsToBlock(programId, blockId, ImmutableList.of(nameQuestionId));
      programService.setProgramQuestionDefinitionOptionality(
          programId, blockId, nameQuestionId, true);

    } catch (ProgramNotFoundException
        | ProgramBlockDefinitionNotFoundException
        | QuestionNotFoundException
        | CantAddQuestionToBlockException
        | ProgramQuestionDefinitionNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public void insertComprehensiveSampleProgram(
      ImmutableList<QuestionDefinition> createdSampleQuestions) {
    try {
      ErrorAnd<ProgramDefinition, CiviFormError> programDefinitionResult =
          programService.createProgramDefinition(
              "comprehensive-sample-program",
              "desc",
              "Comprehensive Sample Program",
              "display description",
              /* defaultConfirmationMessage= */ "",
              "https://github.com/civiform/civiform",
              DisplayMode.PUBLIC.getValue(),
              /* eligibilityIsGating= */ true,
              /* programType= */ ProgramType.DEFAULT,
              /* isIntakeFormFeatureEnabled= */ false,
              ImmutableList.copyOf(new ArrayList<>()));
      if (programDefinitionResult.isError()) {
        throw new RuntimeException(programDefinitionResult.getErrors().toString());
      }
      ProgramDefinition programDefinition = programDefinitionResult.getResult();
      long programId = programDefinition.id();

      ErrorAnd<StatusDefinitions, CiviFormError> appendStatusResult =
          statusService.appendStatus(
              programId,
              StatusDefinitions.Status.builder()
                  .setStatusText("Pending Review")
                  .setDefaultStatus(Optional.of(true))
                  .setLocalizedStatusText(LocalizedStrings.withDefaultValue("Pending Review"))
                  .setLocalizedEmailBodyText(Optional.empty())
                  .build());
      if (appendStatusResult.isError()) {
        throw new RuntimeException(appendStatusResult.getErrors().toString());
      }

      long blockId = 1L;
      BlockForm blockForm = new BlockForm();
      blockForm.setName("Screen 1");
      blockForm.setDescription("one of each question type - part 1");
      programService.updateBlock(programId, blockId, blockForm).getResult();
      programService.addQuestionsToBlock(
          programId,
          blockId,
          ImmutableList.of(
              getCreatedId(STATIC_CONTENT_QUESTION_DEFINITION, createdSampleQuestions),
              getCreatedId(ADDRESS_QUESTION_DEFINITION, createdSampleQuestions),
              getCreatedId(CHECKBOX_QUESTION_DEFINITION, createdSampleQuestions),
              getCreatedId(CURRENCY_QUESTION_DEFINITION, createdSampleQuestions),
              getCreatedId(DATE_QUESTION_DEFINITION, createdSampleQuestions),
              getCreatedId(DROPDOWN_QUESTION_DEFINITION, createdSampleQuestions),
              getCreatedId(PHONE_QUESTION_DEFINITION, createdSampleQuestions)));

      blockId =
          programService.addBlockToProgram(programId).getResult().maybeAddedBlock().get().id();
      blockForm.setName("Screen 2");
      blockForm.setDescription("one of each question type - part 2");
      programService.updateBlock(programId, blockId, blockForm);
      programService.addQuestionsToBlock(
          programId,
          blockId,
          ImmutableList.of(
              getCreatedId(EMAIL_QUESTION_DEFINITION, createdSampleQuestions),
              getCreatedId(ID_QUESTION_DEFINITION, createdSampleQuestions),
              getCreatedId(NAME_QUESTION_DEFINITION, createdSampleQuestions),
              getCreatedId(NUMBER_QUESTION_DEFINITION, createdSampleQuestions),
              getCreatedId(TEXT_QUESTION_DEFINITION, createdSampleQuestions)));

      blockId =
          programService.addBlockToProgram(programId).getResult().maybeAddedBlock().get().id();
      blockForm.setName("enumerator");
      blockForm.setDescription("this is for an enumerator");
      programService.updateBlock(programId, blockId, blockForm);
      long enumeratorId = getCreatedId(ENUMERATOR_QUESTION_DEFINITION, createdSampleQuestions);
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
          ImmutableList.of(
              // This is the only sample question for which we must call create here, because it
              // includes an enumeratorId that only gets generated after
              // ENUMERATOR_QUESTION_DEFINITION is created.
              questionService
                  .create(dateEnumeratedQuestionDefinition(enumeratorId))
                  .getResult()
                  .getId()));

      blockId =
          programService.addBlockToProgram(programId).getResult().maybeAddedBlock().get().id();
      blockForm.setName("Screen 3");
      blockForm.setDescription("Random information");
      programService.updateBlock(programId, blockId, blockForm);
      long radioButtonQuestionId =
          getCreatedId(RADIO_BUTTON_QUESTION_DEFINITION, createdSampleQuestions);
      programService.addQuestionsToBlock(
          programId, blockId, ImmutableList.of(radioButtonQuestionId));

      blockId =
          programService.addBlockToProgram(programId).getResult().maybeAddedBlock().get().id();
      blockForm.setName("Screen with Predicate");
      blockForm.setDescription("May be hidden");
      programService.updateBlock(programId, blockId, blockForm);
      // Add an unanswered question to the block so it is considered incomplete.
      programService.addQuestionsToBlock(
          programId,
          blockId,
          ImmutableList.of(
              getCreatedId(DATE_PREDICATE_QUESTION_DEFINITION, createdSampleQuestions)));
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
      programService.setBlockVisibilityPredicate(programId, blockId, Optional.of(predicate));

      // Add file upload as optional to make local testing easier.
      blockId =
          programService.addBlockToProgram(programId).getResult().maybeAddedBlock().get().id();
      blockForm.setName("file upload");
      blockForm.setDescription("this is for file upload");
      programService.updateBlock(programId, blockId, blockForm);
      long fileQuestionId = getCreatedId(FILE_UPLOAD_QUESTION_DEFINITION, createdSampleQuestions);
      programService.addQuestionsToBlock(programId, blockId, ImmutableList.of(fileQuestionId));
      programService.setProgramQuestionDefinitionOptionality(
          programId, blockId, fileQuestionId, true);

    } catch (ProgramNotFoundException
        | ProgramBlockDefinitionNotFoundException
        | IllegalPredicateOrderingException
        | QuestionNotFoundException
        | CantAddQuestionToBlockException
        | ProgramQuestionDefinitionNotFoundException
        | DuplicateStatusException e) {
      throw new RuntimeException(e);
    }
  }

  /** Seeds the predefined program categories from the category translation files. */
  public List<CategoryModel> seedProgramCategories() {
    CategoryTranslationFileParser parser = new CategoryTranslationFileParser(environment);
    List<CategoryModel> categories = parser.createCategoryModelList();

    List<CategoryModel> dbCategories = new ArrayList<>();
    categories.forEach(
        category -> {
          inSerializableTransaction(
              () -> {
                CategoryModel inserted = categoryRepository.fetchOrSaveUniqueCategory(category);
                if (inserted != null) {
                  dbCategories.add(inserted);
                }
              },
              1);
        });

    return dbCategories;
  }

  private void inSerializableTransaction(Runnable fn, int tryCount) {
    Transaction transaction =
        database.beginTransaction(TxScope.requiresNew().setIsolation(TxIsolation.SERIALIZABLE));

    try {
      fn.run();
      transaction.commit();
    } catch (NonUniqueResultException | SerializableConflictException | RollbackException e) {
      LOGGER.warn("Database seed transaction failed: {}", e);

      if (tryCount > MAX_RETRIES) {
        throw new RuntimeException(e);
      }

      transaction.end();

      long sleepDurMillis = Math.round(Math.pow(2, tryCount)) + new Random().nextInt(100);

      try {
        Thread.sleep(sleepDurMillis);
      } catch (InterruptedException ie) {
        throw new RuntimeException(ie);
      }

      inSerializableTransaction(fn, ++tryCount);
    } finally {
      if (transaction.isActive()) {
        transaction.end();
      }
    }
  }

  /**
   * Gets the seeded question definition's ID from the list of created sample questions. The ID is
   * necessarily not available in {@link SampleQuestionDefinitions}.
   */
  private long getCreatedId(
      QuestionDefinition questionDefinition,
      ImmutableList<QuestionDefinition> createdSampleQuestions) {
    return createdSampleQuestions.stream()
        .filter(q -> q.getName().equals(questionDefinition.getName()))
        .findFirst()
        .map(QuestionDefinition::getId)
        .orElseThrow();
  }
}
