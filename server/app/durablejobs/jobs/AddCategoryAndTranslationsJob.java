package durablejobs.jobs;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import controllers.dev.seeding.CategoryTranslationFileParser;
import durablejobs.DurableJob;
import io.ebean.DB;
import io.ebean.Database;
import io.ebean.Transaction;
import io.ebean.TxScope;
import java.util.List;
import models.CategoryModel;
import models.PersistedDurableJobModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Environment;
import repository.CategoryRepository;
import services.LocalizedStrings;

/* Iterates through all categories and ensures translations aren't missing. */
public final class AddCategoryAndTranslationsJob extends DurableJob {
  private static final Logger LOGGER = LoggerFactory.getLogger(AddCategoryAndTranslationsJob.class);
  private static final ObjectMapper objectMapper = new ObjectMapper();

  private final CategoryRepository categoryRepository;
  private final Environment environment;
  private final PersistedDurableJobModel persistedDurableJobModel;
  private final Database database;

  public AddCategoryAndTranslationsJob(
      CategoryRepository categoryRepository,
      Environment environment,
      PersistedDurableJobModel persistedDurableJobModel) {
    this.categoryRepository = checkNotNull(categoryRepository);
    this.environment = checkNotNull(environment);
    this.persistedDurableJobModel = persistedDurableJobModel;
    this.database = DB.getDefault();
  }

  @Override
  public PersistedDurableJobModel getPersistedDurableJob() {
    return persistedDurableJobModel;
  }

  @Override
  public void run() {
    LOGGER.info("Starting job to update categories with translations.");
    int errorCount = 0;
    try (Transaction jobTransaction = database.beginTransaction()) {
      try {
        // Parse messages files for most up to date translations
        CategoryTranslationFileParser messagesFileParser =
            new CategoryTranslationFileParser(environment);
        List<CategoryModel> categoriesFromMessagesFile =
            messagesFileParser.createCategoryModelList();

        // Iterate through categories from the messages files and compare with categories in the DB
        for (CategoryModel messageFileCategory : categoriesFromMessagesFile) {
          // Get the database category or create one if there is none matching the category from the
          // messages file
          CategoryModel dbCategory =
              categoryRepository.fetchOrSaveUniqueCategory(messageFileCategory);
          LocalizedStrings dbTranslations = dbCategory.getLocalizedName();
          LocalizedStrings fileTranslations = messageFileCategory.getLocalizedName();
          // Update the database if the translations in the messages file differs
          if (!dbTranslations.equals(fileTranslations)) {
            LOGGER.info("Translations mismatch for category ID: {}", dbCategory.id);
            try (Transaction stepTransaction = database.beginTransaction(TxScope.mandatory())) {
              JsonNode localizedNameToSet =
                  objectMapper.readTree(objectMapper.writeValueAsString(fileTranslations));
              categoryRepository.updateCategoryLocalizedName(
                  dbCategory.id, localizedNameToSet.toString());

              stepTransaction.commit();
              LOGGER.debug("Translation change. Updated database.");
            } catch (JsonProcessingException e) {
              errorCount++;
              e.printStackTrace();
            }
          }
        }
      } catch (RuntimeException e) {
        errorCount++;
        LOGGER.error(e.getMessage(), e);
      }

      if (errorCount == 0) {
        LOGGER.info("Finished adding and updating translations for categories.");
        jobTransaction.commit();
      } else {
        LOGGER.error(
            "Failed to update categories and their translations. See previous logs for"
                + " failures. Total failures: {0}",
            errorCount);
        jobTransaction.rollback();
      }
    }
  }
}
