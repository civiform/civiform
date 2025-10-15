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
import repository.CategoryRepository;
import services.LocalizedStrings;

/* Iterates through all categories and ensures translations aren't missing. */
public final class AddCategoryAndTranslationsJob extends DurableJob {
  private static final Logger logger = LoggerFactory.getLogger(AddCategoryAndTranslationsJob.class);

  private final CategoryRepository categoryRepository;
  private final PersistedDurableJobModel persistedDurableJobModel;
  private final ObjectMapper mapper;
  private final Database database;
  private final CategoryTranslationFileParser categoryTranslationFileParser;

  public AddCategoryAndTranslationsJob(
      CategoryRepository categoryRepository,
      PersistedDurableJobModel persistedDurableJobModel,
      ObjectMapper mapper,
      CategoryTranslationFileParser categoryTranslationFileParser) {
    this.categoryRepository = checkNotNull(categoryRepository);
    this.persistedDurableJobModel = persistedDurableJobModel;
    this.mapper = checkNotNull(mapper);
    this.database = DB.getDefault();
    this.categoryTranslationFileParser = checkNotNull(categoryTranslationFileParser);
  }

  @Override
  public PersistedDurableJobModel getPersistedDurableJob() {
    return persistedDurableJobModel;
  }

  @Override
  public void run() {
    logger.info("Starting job to update categories with translations.");
    int errorCount = 0;
    try (Transaction jobTransaction = database.beginTransaction()) {
      try {
        List<CategoryModel> categoriesFromMessagesFile =
            categoryTranslationFileParser.createCategoryModelList();

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
            logger.info("Translations mismatch for category ID: {}", dbCategory.id);
            try (Transaction stepTransaction = database.beginTransaction(TxScope.mandatory())) {
              JsonNode localizedNameToSet =
                  mapper.readTree(mapper.writeValueAsString(fileTranslations));
              categoryRepository.updateCategoryLocalizedName(
                  dbCategory.id, localizedNameToSet.toString());

              stepTransaction.commit();
              logger.debug("Translation change. Updated database.");
            } catch (JsonProcessingException e) {
              errorCount++;
              logger.error(e.getMessage(), e);
            }
          }
        }
      } catch (RuntimeException e) {
        errorCount++;
        logger.error(e.getMessage(), e);
      }

      if (errorCount == 0) {
        logger.info("Finished adding and updating translations for categories.");
        jobTransaction.commit();
      } else {
        logger.error(
            "Failed to update categories and their translations. See previous logs for"
                + " failures. Total failures: {0}",
            errorCount);
        jobTransaction.rollback();
      }
    }
  }
}
