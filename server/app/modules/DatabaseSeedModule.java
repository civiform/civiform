package modules;

import akka.actor.ActorSystem;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import java.time.Duration;
import javax.inject.Provider;
import scala.concurrent.ExecutionContext;
import tasks.DatabaseSeedTask;

/**
 * Binds the {@link DatabaseSeedScheduler} as an eager singleton, which causes it to run at server
 * start time.
 */
public class DatabaseSeedModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(DatabaseSeedScheduler.class).asEagerSingleton();
  }

  public static class DatabaseSeedScheduler {

    @Inject
    public DatabaseSeedScheduler(
        ActorSystem actorSystem,
        ExecutionContext executionContext,
        Provider<DatabaseSeedTask> databaseSeedTaskProvider) {
      actorSystem
          .scheduler()
          .scheduleOnce(
              Duration.ofMillis(10), () -> databaseSeedTaskProvider.get().run(), executionContext);
    }
  }
}
