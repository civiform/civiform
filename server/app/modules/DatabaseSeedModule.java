package modules;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import javax.inject.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.api.db.evolutions.ApplicationEvolutions;
import services.seeding.DatabaseSeedTask;

/**
 * Binds the {@link DatabaseSeedScheduler} as an eager singleton, which causes it to run at server
 * start time.
 */
public final class DatabaseSeedModule extends AbstractModule {
  private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseSeedModule.class);

  @Override
  protected void configure() {
    LOGGER.trace("Module Started");
    bind(DatabaseSeedScheduler.class).asEagerSingleton();
  }

  public static final class DatabaseSeedScheduler {

    @Inject
    public DatabaseSeedScheduler(
        ApplicationEvolutions applicationEvolutions,
        Provider<DatabaseSeedTask> databaseSeedTaskProvider) {
      LOGGER.trace("DatabaseSeedScheduler - Started");

      if (applicationEvolutions.upToDate()) {
        LOGGER.trace("DatabaseSeedScheduler - Task Start");
        databaseSeedTaskProvider.get().run();
        LOGGER.trace("DatabaseSeedScheduler - Task End");
      } else {
        LOGGER.trace("Evolutions Not Ready");
      }
    }
  }
}
