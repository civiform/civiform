package modules;

import com.google.inject.AbstractModule;
import tasks.DatabaseSeedTask;

/**
 * Binds the {@link DatabaseSeedTask} as an eager singleton, which causes it to run at server start
 * time.
 */
public class DatabaseSeedModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(DatabaseSeedTask.class).asEagerSingleton();
  }
}
