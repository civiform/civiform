package models;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import play.i18n.Lang;
import repository.CategoryRepository;
import repository.ResetPostgres;

public class CategoryModelTest extends ResetPostgres {

  private CategoryRepository repo;

  @Before
  public void setupCategoryRepository() {
    repo = instanceOf(CategoryRepository.class);
  }

  @Test
  public void canSaveCategory() {
    ImmutableMap<Locale, String> translations =
        ImmutableMap.of(
            Lang.forCode("en-US").toLocale(), "Health", Lang.forCode("es-US").toLocale(), "Salud");
    CategoryModel category = new CategoryModel(translations);
    category.save();

    CategoryModel found = repo.fetchOrInsertUniqueCategory(category);

    assertThat(found).isEqualTo(category);
  }

  @Test
  public void testTimestamps() throws Exception {
    ImmutableMap<Locale, String> translations =
        ImmutableMap.of(
            Lang.forCode("en-US").toLocale(), "Health", Lang.forCode("es-US").toLocale(), "Salud");
    CategoryModel initialCategory = new CategoryModel(translations);
    initialCategory.save();

    assertThat(initialCategory.getCreateTime()).isNotNull();
    assertThat(initialCategory.getLastModifiedTime()).isNotNull();

    // Ensure a freshly loaded copy has the same timestamps.
    CategoryModel freshlyLoaded = repo.fetchOrInsertUniqueCategory(initialCategory);
    assertThat(freshlyLoaded.getCreateTime()).isEqualTo(initialCategory.getCreateTime());
    assertThat(freshlyLoaded.getLastModifiedTime())
        .isEqualTo(initialCategory.getLastModifiedTime());

    // Update the copy.
    // When persisting models with @WhenModified fields, EBean
    // truncates the persisted timestamp to milliseconds:
    // https://github.com/seattle-uat/civiform/pull/2499#issuecomment-1133325484.
    // Sleep for a few milliseconds to ensure that a subsequent
    // update would have a distinct timestamp.
    TimeUnit.MILLISECONDS.sleep(5);
    freshlyLoaded.markAsDirty();
    freshlyLoaded.save();

    CategoryModel afterUpdate = repo.fetchOrInsertUniqueCategory(initialCategory);
    assertThat(afterUpdate.getCreateTime()).isEqualTo(initialCategory.getCreateTime());
    assertThat(afterUpdate.getLastModifiedTime()).isNotNull();
    assertThat(afterUpdate.getLastModifiedTime()).isAfter(initialCategory.getLastModifiedTime());
  }

  @Test
  public void newCategoryHasActiveLifecycleStage() {
    ImmutableMap<Locale, String> translations =
        ImmutableMap.of(
            Lang.forCode("en-US").toLocale(), "Health", Lang.forCode("es-US").toLocale(), "Salud");
    CategoryModel category = new CategoryModel(translations);
    assertThat(category.getLifecycleStage()).isEqualTo(LifecycleStage.ACTIVE);
  }
}
