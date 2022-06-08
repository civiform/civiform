package modules;

import com.google.inject.AbstractModule;
import tasks.DatabaseSeedTask;

public class DatabaseSeedModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(DatabaseSeedTask.class).asEagerSingleton();
  }
}
