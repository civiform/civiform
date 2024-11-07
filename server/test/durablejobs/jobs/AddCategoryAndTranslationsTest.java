package durablejobs.jobs;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.time.Instant;
import java.util.Locale;
import models.CategoryModel;
import models.JobType;
import models.PersistedDurableJobModel;
import org.junit.Test;
import play.Environment;
import play.Mode;
import play.i18n.Lang;
import repository.CategoryRepository;
import repository.ResetPostgres;
import services.LocalizedStrings;
import services.TranslationNotFoundException;

public class AddCategoryAndTranslationsTest extends ResetPostgres {
  @Test
  public void run_updatesCategoriesIfTranslationsAreDifferent()
      throws TranslationNotFoundException {
    ImmutableMap<Locale, String> translations =
        ImmutableMap.of(
            Lang.forCode("en-US").toLocale(),
            "Healthcare",
            Lang.forCode("es-US").toLocale(),
            "Incorrect health");
    CategoryModel category = resourceCreator.insertCategory(translations);
    runJob();

    category.refresh();
    assertThat(category.getLocalizedName().get(Lang.forCode("es-US").toLocale()))
        .isEqualTo("Salud");
  }

  @Test
  public void run_updatesCategoriesIfTranslationsAreNotPresent()
      throws TranslationNotFoundException {
    ImmutableMap<Locale, String> translations =
        ImmutableMap.of(
            Lang.forCode("en-US").toLocale(),
            "Healthcare",
            Lang.forCode("es-US").toLocale(),
            "Salud");
    CategoryModel category = resourceCreator.insertCategory(translations);
    runJob();

    category.refresh();
    assertThat(category.getLocalizedName().get(Lang.forCode("fr").toLocale())).isEqualTo("Santé");
  }

  @Test
  public void run_updatesCategoriesAndCreatesNew() throws TranslationNotFoundException {
    ImmutableMap<Locale, String> translations =
        ImmutableMap.of(
            Lang.forCode("en-US").toLocale(),
            "Healthcare",
            Lang.forCode("es-US").toLocale(),
            "Incorrect health");
    CategoryModel category = resourceCreator.insertCategory(translations);
    runJob();

    category.refresh();
    // Updates incorrect translation
    assertThat(category.getLocalizedName().get(Lang.forCode("es-US").toLocale()))
        .isEqualTo("Salud");
    // Adds translation that wasn't present in DB
    assertThat(category.getLocalizedName().get(Lang.forCode("fr").toLocale())).isEqualTo("Santé");
  }

  @Test
  public void run_makesNoUpdatesIfCategoriesMatch() {
    ImmutableList<CategoryModel> parsedCategories = resourceCreator.insertCategoriesFromParser();
    CategoryModel firstCategory = parsedCategories.get(1);
    LocalizedStrings localizedNameBeforeRun = firstCategory.getLocalizedName();
    Instant lastModifiedTimeBeforeRun = firstCategory.getLastModifiedTime();
    runJob();

    firstCategory.refresh();
    assertThat(firstCategory.getLastModifiedTime()).isEqualTo(lastModifiedTimeBeforeRun);
    assertThat(firstCategory.getLocalizedName()).isEqualTo(localizedNameBeforeRun);
  }

  private void runJob() {
    Environment environment = new Environment(Mode.PROD);
    AddCategoryAndTranslationsJob job =
        new AddCategoryAndTranslationsJob(
            instanceOf(CategoryRepository.class),
            environment,
            new PersistedDurableJobModel("fake-job", JobType.RUN_ONCE, Instant.now()));

    job.run();
  }
}
