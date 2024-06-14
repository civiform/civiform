package repository;

import com.google.common.collect.ImmutableList;
import io.ebean.DB;
import io.ebean.Database;
import javax.inject.Inject;
import models.CategoryModel;

public final class CategoryRepository {

  private static final QueryProfileLocationBuilder queryProfileLocationBuilder =
      new QueryProfileLocationBuilder("CategoryRepository");

  private final Database database;

  @Inject
  public CategoryRepository() {
    this.database = DB.getDefault();
  }

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

  public ImmutableList<CategoryModel> listCategories() {
    return ImmutableList.copyOf(
        database
            .find(CategoryModel.class)
            .setLabel("CategoryModel.list")
            .setProfileLocation(queryProfileLocationBuilder.create("listCategories"))
            .findList());
  }

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
