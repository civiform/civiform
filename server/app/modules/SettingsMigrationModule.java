package modules;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import javax.inject.Provider;
import play.api.db.evolutions.ApplicationEvolutions;
import services.settings.SettingsService;

/**
 * Migrate ADMIN_WRITEABLE server settings from the Play Config system to database-backed admin
 * settings. Runs each time the server is started.
 */
public class SettingsMigrationModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(SettingsMigrator.class).asEagerSingleton();
  }

  public static final class SettingsMigrator {

    @Inject
    public SettingsMigrator(
        ApplicationEvolutions applicationEvolutions,
        Provider<SettingsService> settingsServiceProvider) {

      if (applicationEvolutions.upToDate()) {
        settingsServiceProvider.get().migrateConfigValuesToSettingsGroup();
      }
    }
  }
}
