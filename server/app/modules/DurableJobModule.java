package modules;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import durablejobs.DurableJobName;
import durablejobs.DurableJobRegistry;
import durablejobs.RecurringJobSchedulers;
import durablejobs.jobs.OldJobCleanupJob;
import repository.PersistedDurableJobRepository;

/**
 * Configures {@link durablejobs.DurableJob}s with their {@link DurableJobName} and, if they are
 * recurring, their {@link durablejobs.RecurringJobExecutionTimeResolver}.
 *
 * <p>NOTE: THIS SYSTEM IS STILL UNDER DEVELOPMENT AND THIS MODULE IS NOT CURRENTLY ENABLED IN
 * application.conf TODO(https://github.com/civiform/civiform/issues/4191): Enable DurableJobModule
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
        new RecurringJobSchedulers.Sunday2Am());

    return durableJobRegistry;
  }
}
