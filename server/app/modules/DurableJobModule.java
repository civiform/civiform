package modules;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import jobs.DurableJobName;
import jobs.DurableJobRegistry;
import jobs.OldJobCleanupJob;
import jobs.RecurringJobSchedulers;
import repository.PersistedDurableJobRepository;

/**
 * Configures {@link jobs.DurableJob}s with their {@link DurableJobName} and, if they are recurring,
 * their {@link jobs.RecurringJobExecutionTimeResolver}.
 *
 * <p>NOTE: THIS SYSTEM IS STILL UNDER DEVELOPMENT AND THIS MODULE IS NOT CURRENTLY ENABLED IN
 * application.conf
 */
public final class DurableJobModule extends AbstractModule {

  @Provides
  public DurableJobRegistry provideDurableJobRegistry(
      PersistedDurableJobRepository persistedDurableJobRepository) {
    var durableJobRegistry = new DurableJobRegistry();

    durableJobRegistry.register(
        DurableJobName.OLD_JOB_CLEANUP,
        persistedDurableJob ->
            new OldJobCleanupJob(persistedDurableJobRepository, persistedDurableJob),
        new RecurringJobSchedulers.EverySunday2Am());

    return durableJobRegistry;
  }
}
