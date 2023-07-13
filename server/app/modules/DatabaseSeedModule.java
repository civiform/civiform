package modules;

import akka.actor.ActorSystem;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import java.time.Duration;
import javax.inject.Provider;
import scala.concurrent.ExecutionContext;
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
        ActorSystem actorSystem,
        ExecutionContext executionContext,
        Provider<DatabaseSeedTask> databaseSeedTaskProvider) {
      actorSystem
          .scheduler()
          .scheduleOnce(
              // schedule seed task for 5 sec from now. There is a race condition
              // with Play evolutions. Evolutions must run before we seed database.
              // It doesn't seem to be a way to run code after evolutions so just
              // give them few sec to run.
              Duration.ofSeconds(5), () -> databaseSeedTaskProvider.get().run(), executionContext);
    }
  }
}
