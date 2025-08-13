package repository;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import com.google.common.annotations.VisibleForTesting;
import io.ebean.DB;
import io.ebean.Database;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.SettingsGroupModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.cache.NamedCache;
import play.cache.SyncCacheApi;
import services.settings.SettingsManifest;

/** Contains queries related to the server settings system. */
public final class SettingsGroupRepository {
  @VisibleForTesting static final String CURRENT_SETTINGS_CACHE_KEY = "current-settings";
  private static final Logger logger = LoggerFactory.getLogger(SettingsGroupRepository.class);

  private static final QueryProfileLocationBuilder queryProfileLocationBuilder =
      new QueryProfileLocationBuilder("SettingsGroupRepository");

  private final Database database;
  private final DatabaseExecutionContext databaseExecutionContext;
  private final SettingsManifest settingsManifest;
  private final SyncCacheApi settingsCache;

  @Inject
  public SettingsGroupRepository(
      DatabaseExecutionContext databaseExecutionContext,
      SettingsManifest settingsManifest,
      @NamedCache("civiform-settings") SyncCacheApi settingsCache) {
    this.database = DB.getDefault();
    this.databaseExecutionContext = checkNotNull(databaseExecutionContext);
    this.settingsManifest = checkNotNull(settingsManifest);
    this.settingsCache = checkNotNull(settingsCache);
  }

  /** Get the most recently created {@link SettingsGroupModel}. */
  public CompletionStage<Optional<SettingsGroupModel>> getCurrentSettings() {
    return supplyAsync(
        () ->
            settingsManifest.getSettingsCacheEnabled()
                ? settingsCache.getOrElseUpdate(
                    CURRENT_SETTINGS_CACHE_KEY, this::findCurrentSettingsFromDb)
                : findCurrentSettingsFromDb(),
        databaseExecutionContext);
  }

  private Optional<SettingsGroupModel> findCurrentSettingsFromDb() {
    return database
        .find(SettingsGroupModel.class)
        .orderBy()
        .desc("create_time")
        .setMaxRows(1)
        .setLabel("SettingsGroupModel.findOne")
        .setProfileLocation(queryProfileLocationBuilder.create("getCurrentSettings"))
        .findOneOrEmpty();
  }

  /** Clears the cache for the current settings. This should be called after updating settings. */
  public void clearCurrentSettingsCache() {
    logger.debug("Clearing current settings cache");
    settingsCache.remove(CURRENT_SETTINGS_CACHE_KEY);
  }
}
