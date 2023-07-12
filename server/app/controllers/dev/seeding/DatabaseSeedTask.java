package controllers.dev.seeding;

import static com.google.common.base.Preconditions.checkNotNull;
import static controllers.dev.seeding.SampleQuestionDefinitions.DATE_QUESTION_DEFINITION;
import static controllers.dev.seeding.SampleQuestionDefinitions.NAME_QUESTION_DEFINITION;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.ebean.DB;
import io.ebean.Database;
import io.ebean.SerializableConflictException;
import io.ebean.Transaction;
import io.ebean.TxScope;
import io.ebean.annotation.TxIsolation;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.persistence.NonUniqueResultException;
import javax.persistence.RollbackException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repository.VersionRepository;
import services.CiviFormError;
import services.ErrorAnd;
import services.question.QuestionService;
import services.question.types.QuestionDefinition;

/**
 * Task for seeding the database.
 *
 * <p>Logic for seeding different resources should be factored into separate methods for clarity.
 *
 * <p>To avoid attempting to insert duplicate resources it uses a thread-local database transaction
 * with a transaction scope of SERIALIZABLE. If the transaction fails it retries up to {@code
 * MAX_RETRIES} times, sleeping the thread with exponential backoff + jitter before each retry.
 *
 * <p>If the task fails {@code MAX_RETRIES} times, it throws a {@code RuntimeException} wrapping the
 * exception causing it to fail. This will not cause the server itself to crash or prevent it from
 * starting and serving requests.
 */
public final class DatabaseSeedTask {

  private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseSeedTask.class);
  private static final int MAX_RETRIES = 10;
  private static final ImmutableList<QuestionDefinition> SAMPLE_QUESTIONS =
      ImmutableList.of(NAME_QUESTION_DEFINITION, DATE_QUESTION_DEFINITION);

  private final QuestionService questionService;
  private final VersionRepository versionRepository;
  private final Database database;

  @Inject
  public DatabaseSeedTask(QuestionService questionService, VersionRepository versionRepository) {
    this.questionService = checkNotNull(questionService);
    this.versionRepository = checkNotNull(versionRepository);
    this.database = DB.getDefault();
  }

  public ImmutableList<QuestionDefinition> run() {
    return seedQuestions();
  }

  /**
   * Ensures that questions with names matching those in {@code SAMPLE_QUESTIONS} are present in the
   * database, inserting the definitions in {@code SAMPLE_QUESTIONS} if any aren't found.
   */
  private ImmutableList<QuestionDefinition> seedQuestions() {
    ImmutableSet<String> sampleQuestionNames =
        SAMPLE_QUESTIONS.stream()
            .map(QuestionDefinition::getName)
            .collect(ImmutableSet.toImmutableSet());
    ImmutableMap<String, QuestionDefinition> existingCanonicalQuestions =
        questionService.getExistingQuestions(sampleQuestionNames);
    if (existingCanonicalQuestions.size() < sampleQuestionNames.size()) {
      // Ensure a draft version exists to avoid transaction collisions with getDraftVersion.
      versionRepository.getDraftVersion();
    }

    ImmutableList.Builder<QuestionDefinition> questionDefinitions = ImmutableList.builder();
    for (QuestionDefinition questionDefinition : SAMPLE_QUESTIONS) {
      if (existingCanonicalQuestions.containsKey(questionDefinition.getName())) {
        LOGGER.info(
            "Canonical question \"%s\" exists at server start", questionDefinition.getName());
        questionDefinitions.add(existingCanonicalQuestions.get(questionDefinition.getName()));
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
              "Unable to create canonical question \"%s\" due to %s",
              questionDefinition.getName(), errorMessages));
      return Optional.empty();
    } else {
      LOGGER.info("Created canonical question \"%s\"", questionDefinition.getName());
      return Optional.of(result.getResult());
    }
  }

  private void inSerializableTransaction(Runnable fn, int tryCount) {
    Transaction transaction =
        database.beginTransaction(TxScope.requiresNew().setIsolation(TxIsolation.SERIALIZABLE));

    try {
      fn.run();
      transaction.commit();
    } catch (NonUniqueResultException | SerializableConflictException | RollbackException e) {
      LOGGER.warn("Database seed transaction failed: %s", e);

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
}
