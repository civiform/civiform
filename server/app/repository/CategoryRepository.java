package repository;

import com.google.common.collect.ImmutableList;
import io.ebean.DB;
import io.ebean.Database;
import models.CategoryModel;
import models.ProgramModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.cache.NamedCache;
import play.cache.SyncCacheApi;
import services.settings.SettingsManifest;

import javax.inject.Inject;
import javax.inject.Provider;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

public final class CategoryRepository {
  private static final Logger logger = LoggerFactory.getLogger(CategoryRepository.class);
  private static final QueryProfileLocationBuilder queryProfileLocationBuilder =
    new QueryProfileLocationBuilder("CategoryRepository");

  private final Database database;
  private final DatabaseExecutionContext executionContext;
  private final SettingsManifest settingsManifest;

  @Inject
  public CategoryRepository(
    DatabaseExecutionContext databaseExecutionContext,
    SettingsManifest settingsManifest) {
    this.database = DB.getDefault();
    this.executionContext = checkNotNull(databaseExecutionContext);
    this.settingsManifest = checkNotNull(settingsManifest);
  }

  public List<CategoryModel> findCategoriesByIds(List<Long> ids) {
    return database
      .find(CategoryModel.class)
      .setLabel("CategoryModel.findByIds")
      .setProfileLocation(queryProfileLocationBuilder.create("findCategoriesByIds"))
      .where()
      .idIn(ids)
      .findList();
  }

  public List<CategoryModel> listCategories() {
    return database
      .find(CategoryModel.class)
      .setLabel("CategoryModel.list")
      .setProfileLocation(queryProfileLocationBuilder.create("listCategories"))
      .findList();
  }

  public CategoryModel fetchOrInsertUniqueCategory(CategoryModel category) {
    CategoryModel existing =
      database
      .find(CategoryModel.class)
      .where()
        .jsonEqualTo("localizedName", "translations.en_US", category.getDefaultName())
      .findOne();
    if (existing != null) {
      return existing;
    }
    return insertCategory(category);
  }
  private CategoryModel insertCategory(CategoryModel category) {
    category.id = null;
    database.insert(category);
    category.refresh();
    return category;
  }

}
