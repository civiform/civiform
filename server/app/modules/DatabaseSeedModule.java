package modules;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import javax.inject.Provider;
import play.api.db.evolutions.ApplicationEvolutions;
import services.seeding.DatabaseSeedTask;

/**
 * Binds the {@link DatabaseSeedScheduler} as an eager singleton, which causes it to run at server
 * start time.
 */
public final class DatabaseSeedModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(DatabaseSeedScheduler.class).asEagerSingleton();
  }

  public static final class DatabaseSeedScheduler {

    @Inject
    public DatabaseSeedScheduler(
        ApplicationEvolutions applicationEvolutions,
        Provider<DatabaseSeedTask> databaseSeedTaskProvider) {

      if (applicationEvolutions.upToDate()) {
        databaseSeedTaskProvider.get().run();
      }
    }
  }
}
