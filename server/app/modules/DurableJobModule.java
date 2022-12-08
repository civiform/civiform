package modules;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import jobs.DurableJobName;
import jobs.DurableJobRegistry;
import jobs.OldJobCleanupJob;
import jobs.RecurringJobSchedulers;

public class DurableJobModule extends AbstractModule {

  @Provides
  public DurableJobRegistry provideDurableJobRegistry() {
    var durableJobRegistry = new DurableJobRegistry();

    durableJobRegistry.register(
        DurableJobName.OLD_JOB_CLEANUP,
        persistedDurableJob -> new OldJobCleanupJob(persistedDurableJob),
        RecurringJobSchedulers::weekly);

    return durableJobRegistry;
  }
}
