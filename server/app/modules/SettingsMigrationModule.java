package modules;

import akka.actor.ActorSystem;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import java.time.Duration;
import javax.inject.Provider;
import scala.concurrent.ExecutionContext;
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
        ActorSystem actorSystem,
        ExecutionContext executionContext,
        Provider<SettingsService> settingsServiceProvider) {
      actorSystem
          .scheduler()
          .scheduleOnce(
              // schedule seed task for 5 sec from now. There is a race condition
              // with Play evolutions. Evolutions must run before we seed database.
              // It doesn't seem to be a way to run code after evolutions so just
              // give them few sec to run.
              Duration.ofSeconds(5),
              () -> settingsServiceProvider.get().migrateConfigValuesToSettingsGroup(),
              executionContext);
    }
  }
}
