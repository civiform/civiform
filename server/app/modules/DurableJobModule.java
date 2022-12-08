package modules;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import jobs.DurableJobName;
import jobs.DurableJobRegistry;
import jobs.OldJobCleanupJob;
import jobs.RecurringJobSchedulers;
import repository.PersistedDurableJobRepository;

public final class DurableJobModule extends AbstractModule {

  @Provides
  public DurableJobRegistry provideDurableJobRegistry(
      PersistedDurableJobRepository persistedDurableJobRepository) {
    var durableJobRegistry = new DurableJobRegistry();

    durableJobRegistry.register(
        DurableJobName.OLD_JOB_CLEANUP,
        persistedDurableJob ->
            new OldJobCleanupJob(persistedDurableJobRepository, persistedDurableJob),
        RecurringJobSchedulers::everySundayAt2Am);

    return durableJobRegistry;
  }
}
