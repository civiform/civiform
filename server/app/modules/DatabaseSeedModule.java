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
  private static final Logger logger = LoggerFactory.getLogger(DatabaseSeedModule.class);

  @Override
  protected void configure() {
    logger.trace("Module Started");
    bind(DatabaseSeedScheduler.class).asEagerSingleton();
  }

  /**
   * This class injects ApplicationEvolutions and checks the `upToDate` method to prevent this
   * module from running until after the evolutions are completed.
   *
   * <p>See <a href="https://github.com/civiform/civiform/pull/8253">PR 8253</a> for more extensive
   * details.
   */
  public static final class DatabaseSeedScheduler {

    @Inject
    public DatabaseSeedScheduler(
        ApplicationEvolutions applicationEvolutions,
        Provider<DatabaseSeedTask> databaseSeedTaskProvider) {
      logger.trace("DatabaseSeedScheduler - Started");

      if (applicationEvolutions.upToDate()) {
        logger.trace("DatabaseSeedScheduler - Task Start");
        databaseSeedTaskProvider.get().run();
        logger.trace("DatabaseSeedScheduler - Task End");
      } else {
        logger.trace("Evolutions Not Ready");
      }
    }
  }
}
