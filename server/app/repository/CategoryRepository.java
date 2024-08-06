package repository;

import com.google.common.collect.ImmutableList;
import io.ebean.DB;
import io.ebean.Database;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javax.inject.Inject;
import models.CategoryModel;

/** A repository object to perform queries on program categories, which are used for filtering. */
public final class CategoryRepository {

  private static final QueryProfileLocationBuilder queryProfileLocationBuilder =
      new QueryProfileLocationBuilder("CategoryRepository");

  private final Database database;

  @Inject
  public CategoryRepository() {
    this.database = DB.getDefault();
  }

  /** Fetches a list of categories with the given IDs. */
  public ImmutableList<CategoryModel> findCategoriesByIds(ImmutableList<Long> ids) {
    return ImmutableList.copyOf(
        database
            .find(CategoryModel.class)
            .setLabel("CategoryModel.findByIds")
            .setProfileLocation(queryProfileLocationBuilder.create("findCategoriesByIds"))
            .where()
            .idIn(ids)
            .findList());
  }

  /** Fetches a list of all categories. */
  public ImmutableList<CategoryModel> listCategories() {
    List<CategoryModel> categories =
        database
            .find(CategoryModel.class)
            .setLabel("CategoryModel.list")
            .setProfileLocation(queryProfileLocationBuilder.create("listCategories"))
            .findList();
    Comparator<CategoryModel> comparator =
        (c1, c2) ->
            c1.getDefaultName()
                .toLowerCase(Locale.getDefault())
                .compareTo(c2.getDefaultName().toLowerCase(Locale.getDefault()));
    categories.sort(comparator);
    return ImmutableList.copyOf(categories);
  }

  /**
   * Fetches the category with the given default name if such a category exists. Otherwise, creates
   * a new category and saves it to the database.
   */
  public CategoryModel fetchOrSaveUniqueCategory(CategoryModel category) {
    CategoryModel existing =
        database
            .find(CategoryModel.class)
            .where()
            .jsonEqualTo("localizedName", "translations.en_US", category.getDefaultName())
            .findOne();
    if (existing != null) {
      return existing;
    }
    return saveCategory(category);
  }

  private CategoryModel saveCategory(CategoryModel category) {
    category.id = null;
    database.save(category);
    category.refresh();
    return category;
  }
}
