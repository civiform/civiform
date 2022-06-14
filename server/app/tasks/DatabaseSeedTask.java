package tasks;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.ebean.DB;
import io.ebean.Database;
import io.ebean.SerializableConflictException;
import io.ebean.Transaction;
import io.ebean.TxScope;
import io.ebean.annotation.TxIsolation;
import java.util.Random;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.persistence.NonUniqueResultException;
import javax.persistence.RollbackException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.i18n.Lang;
import repository.VersionRepository;
import services.CiviFormError;
import services.ErrorAnd;
import services.LocalizedStrings;
import services.question.QuestionService;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionType;

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
  private static final ImmutableList<QuestionDefinition> CANONICAL_QUESTIONS =
      ImmutableList.of(
          new QuestionDefinitionBuilder()
              .setQuestionType(QuestionType.NAME)
              .setName("Applicant Name")
              .setDescription("The applicant's name")
              .setQuestionText(
                  LocalizedStrings.of(
                      ImmutableMap.of(
                          Lang.forCode("am").toLocale(),
                          "ስም (የመጀመሪያ ስም እና የመጨረሻ ስም አህጽሮት ይሆናል)",
                          Lang.forCode("ko").toLocale(),
                          "성함 (이름 및 성의 경우 이니셜도 괜찮음)",
                          Lang.forCode("so").toLocale(),
                          "Magaca (magaca koowaad iyo kan dambe okay)",
                          Lang.forCode("tl").toLocale(),
                          "Pangalan (unang pangalan at ang unang titik ng apilyedo ay okay)",
                          Lang.forCode("vi").toLocale(),
                          "Tên (tên và họ viết tắt đều được)",
                          Lang.forCode("en-US").toLocale(),
                          "Please enter your first and last name",
                          Lang.forCode("es-US").toLocale(),
                          "Nombre (nombre y la inicial del apellido está bien)",
                          Lang.forCode("zh-TW").toLocale(),
                          "姓名（名字和姓氏第一個字母便可）")))
              .unsafeBuild(),
          new QuestionDefinitionBuilder()
              .setQuestionType(QuestionType.DATE)
              .setName("Applicant Date of Birth")
              .setDescription("Applicant's date of birth")
              .setQuestionText(
                  LocalizedStrings.of(
                      Lang.forCode("en-US").toLocale(),
                      "Please enter your date of birth in the format mm/dd/yyyy"))
              .unsafeBuild());

  private final QuestionService questionService;
  private final VersionRepository versionRepository;
  private final Database database;

  @Inject
  public DatabaseSeedTask(QuestionService questionService, VersionRepository versionRepository) {
    this.questionService = checkNotNull(questionService);
    this.versionRepository = checkNotNull(versionRepository);
    this.database = DB.getDefault();
  }

  public void run() {
    seedCanonicalQuestions();
  }

  /**
   * Ensures that questions with names matching those in {@code CANONICAL_QUESTIONS} are present in
   * the database, inserting the definitions in {@code CANONICAL_QUESTIONS} if any aren't found.
   */
  private void seedCanonicalQuestions() {
    if (canonicalQuestionsAlreadyPresent()) {
      return;
    }

    // Ensure a draft version exists to avoid transaction collisions with getDraftVersion.
    versionRepository.getDraftVersion();

    for (QuestionDefinition questionDefinition : CANONICAL_QUESTIONS) {
      inSerializableTransaction(() -> ensureQuestion(questionDefinition), 1);
    }
  }

  private void ensureQuestion(QuestionDefinition questionDefinition) {
    if (questionService.questionExists(questionDefinition.getName())) {
      LOGGER.info("Canonical question \"%s\" exists at server start", questionDefinition.getName());
      return;
    }

    ErrorAnd<QuestionDefinition, CiviFormError> result = questionService.create(questionDefinition);

    if (result.isError()) {
      String errorMessages =
          result.getErrors().stream().map(CiviFormError::message).collect(Collectors.joining(", "));

      LOGGER.error(
          String.format(
              "Unable to create canonical question \"%s\" due to %s",
              questionDefinition.getName(), errorMessages));
    } else {
      LOGGER.info("Created canonical question \"%s\"", questionDefinition.getName());
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

  private boolean canonicalQuestionsAlreadyPresent() {
    return questionService
        .getQuestionNames()
        .containsAll(
            CANONICAL_QUESTIONS.stream()
                .map(QuestionDefinition::getName)
                .collect(ImmutableList.toImmutableList()));
  }
}
