package repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import models.CategoryModel;
import org.junit.Before;
import org.junit.Test;
import play.i18n.Lang;

public class CategoryRepositoryTest extends ResetPostgres {

  private CategoryRepository repo;

  @Before
  public void setupCategoryRepository() {
    repo = instanceOf(CategoryRepository.class);
  }

  @Test
  public void fetchOrInsertUniqueCategory_insertsNewCategory() {
    CategoryModel category = createCategory("Housing", "Vivienda");
    CategoryModel insertedCategory = repo.fetchOrSaveUniqueCategory(category);

    assertThat(insertedCategory.getId()).isNotNull();
    assertThat(insertedCategory.getDefaultName()).isEqualTo("Housing");
    assertThat(insertedCategory.getLocalizedName().hasTranslationFor(Locale.US)).isTrue();
  }

  @Test
  public void listCategories_fetchesAllCategories() {
    CategoryModel category1 = createCategory("Health", "Salud");
    repo.fetchOrSaveUniqueCategory(category1);

    CategoryModel category2 = createCategory("Education", "Educación");
    repo.fetchOrSaveUniqueCategory(category2);

    ImmutableList<CategoryModel> allCategories = repo.listCategories();

    assertThat(allCategories).hasSize(2);
  }

  @Test
  public void fetchOrInsertUniqueCategory_fetchesExistingCategory() {
    CategoryModel category = createCategory("Health", "Salud");
    CategoryModel insertedCategory = repo.fetchOrSaveUniqueCategory(category);
    // Since the category already exists, the method should fetch the existing category rather than
    // creating a new one.
    CategoryModel fetchedCategory = repo.fetchOrSaveUniqueCategory(category);

    assertThat(fetchedCategory.getId()).isEqualTo(insertedCategory.getId());
  }

  @Test
  public void findCategoriesByIds_fetchesCategoriesByIds() {
    CategoryModel category1 = createCategory("Health", "Salud");
    CategoryModel insertedCategory1 = repo.fetchOrSaveUniqueCategory(category1);

    CategoryModel category2 = createCategory("Education", "Educación");
    CategoryModel insertedCategory2 = repo.fetchOrSaveUniqueCategory(category2);

    ImmutableList<CategoryModel> fetchedCategories =
        repo.findCategoriesByIds(
            ImmutableList.of(insertedCategory1.getId(), insertedCategory2.getId()));

    assertThat(fetchedCategories).containsExactlyInAnyOrder(insertedCategory1, insertedCategory2);
  }

  private CategoryModel createCategory(String englishName, String spanishName) {
    ImmutableMap<Locale, String> translations =
        ImmutableMap.of(
            Lang.forCode("en-US").toLocale(),
            englishName,
            Lang.forCode("es-US").toLocale(),
            spanishName);
    return new CategoryModel(translations);
  }
}
