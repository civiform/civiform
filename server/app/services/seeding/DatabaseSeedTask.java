package services.seeding;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import controllers.dev.seeding.CategoryTranslationFileParser;
import io.ebean.DB;
import io.ebean.Database;
import io.ebean.SerializableConflictException;
import io.ebean.Transaction;
import io.ebean.TxScope;
import io.ebean.annotation.TxIsolation;
import jakarta.persistence.NonUniqueResultException;
import jakarta.persistence.RollbackException;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import javax.inject.Inject;
import models.CategoryModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Environment;
import play.i18n.Lang;
import repository.CategoryRepository;
import repository.VersionRepository;
import services.CiviFormError;
import services.ErrorAnd;
import services.LocalizedStrings;
import services.question.QuestionService;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionType;

/**
 * Task for seeding the production database.
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

  public static final QuestionDefinition CANONICAL_NAME_QUESTION =
      new QuestionDefinitionBuilder()
          .setQuestionType(QuestionType.NAME)
          .setName("Name")
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
                      Lang.forCode("lo").toLocale(),
                      "ຊື່ (ນາມສະກຸນ ແລະ ຕົວອັກສອນທຳອິດຂອງນາມສະກຸນແມ່ນຖືກຕ້ອງ)",
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
          .unsafeBuild();
  public static final QuestionDefinition CANONICAL_DOB_QUESTION =
      new QuestionDefinitionBuilder()
          .setQuestionType(QuestionType.DATE)
          .setName("Applicant Date of Birth")
          .setDescription("Applicant's date of birth")
          .setQuestionText(
              LocalizedStrings.of(
                  Lang.forCode("en-US").toLocale(),
                  "Please enter your date of birth in the format mm/dd/yyyy"))
          .unsafeBuild();

  private static final ImmutableList<QuestionDefinition> CANONICAL_QUESTIONS =
      ImmutableList.of(CANONICAL_NAME_QUESTION, CANONICAL_DOB_QUESTION);

  private final QuestionService questionService;
  private final VersionRepository versionRepository;
  private final CategoryRepository categoryRepository;
  private final Database database;
  private final Environment environment;

  @Inject
  public DatabaseSeedTask(
      QuestionService questionService,
      VersionRepository versionRepository,
      CategoryRepository categoryRepository,
      Environment environment) {
    this.questionService = checkNotNull(questionService);
    this.versionRepository = checkNotNull(versionRepository);
    this.categoryRepository = checkNotNull(categoryRepository);
    this.database = DB.getDefault();
    this.environment = checkNotNull(environment);
  }

  public ImmutableList<QuestionDefinition> run() {
    seedProgramCategories();
    return seedCanonicalQuestions();
  }

  /**
   * Ensures that questions with names matching those in {@code CANONICAL_QUESTIONS} are present in
   * the database, inserting the definitions in {@code CANONICAL_QUESTIONS} if any aren't found.
   */
  private ImmutableList<QuestionDefinition> seedCanonicalQuestions() {
    ImmutableSet<String> canonicalQuestionNames =
        CANONICAL_QUESTIONS.stream()
            .map(QuestionDefinition::getName)
            .collect(ImmutableSet.toImmutableSet());
    ImmutableMap<String, QuestionDefinition> existingCanonicalQuestions =
        questionService.getExistingQuestions(canonicalQuestionNames);
    if (existingCanonicalQuestions.size() < canonicalQuestionNames.size()) {
      // Ensure a draft version exists to avoid transaction collisions with getDraftVersion.
      versionRepository.getDraftVersionOrCreate();
    }

    ImmutableList.Builder<QuestionDefinition> questionDefinitions = ImmutableList.builder();
    for (QuestionDefinition questionDefinition : CANONICAL_QUESTIONS) {
      if (existingCanonicalQuestions.containsKey(questionDefinition.getName())) {
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
      LOGGER.info("Created canonical question \"{}\"", questionDefinition.getName());
      return Optional.of(result.getResult());
    }
  }

  /** Seeds the predefined program categories from the category translation files. */
  private void seedProgramCategories() {
    if (categoryRepository.listCategories().isEmpty()) {
      try {
        CategoryTranslationFileParser parser = new CategoryTranslationFileParser(environment);
        List<CategoryModel> categories = parser.createCategoryModelList();

        categories.forEach(
            category -> {
              inSerializableTransaction(
                  () -> {
                    categoryRepository.fetchOrSaveUniqueCategory(category);
                  },
                  1);
            });
      } catch (RuntimeException e) {
        // We don't want to prevent startup if seeding fails.
        LOGGER.error("Failed to seed program categories.", e);
      }
    }
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
}
