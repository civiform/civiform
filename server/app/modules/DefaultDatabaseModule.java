package modules;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import io.ebean.DB;
import io.ebean.Database;

public class DefaultDatabaseModule extends AbstractModule {
  @Provides
  Database provideDatabase() {
    return DB.getDefault();
  }
}
