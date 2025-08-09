package services;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.inject.Inject;
import com.google.inject.Provider;
import play.inject.ApplicationLifecycle;
import repository.DatabaseExecutionContext;
import repository.SettingsGroupRepository;
import services.settings.SettingsCache;

public class SettingsCacheProvider implements Provider<SettingsCache> {
  private final SettingsGroupRepository settingsGroupRepository;
  private final DatabaseExecutionContext dbExecutionContext;
  private final ApplicationLifecycle lifecycle;

  @Inject
  public SettingsCacheProvider(
      SettingsGroupRepository settingsGroupRepository,
      DatabaseExecutionContext dbExecutionContext,
      ApplicationLifecycle lifecycle) {
    this.settingsGroupRepository = checkNotNull(settingsGroupRepository);
    this.dbExecutionContext = checkNotNull(dbExecutionContext);
    this.lifecycle = checkNotNull(lifecycle);
  }

  @Override
  public SettingsCache get() {
    SettingsCache settingsCache =
        new SettingsCache(settingsGroupRepository, dbExecutionContext, lifecycle);

    settingsCache.init();

    return settingsCache;
  }
}
