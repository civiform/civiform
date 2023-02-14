package modules;

import akka.actor.ActorSystem;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.typesafe.config.Config;
import durablejobs.DurableJobName;
import durablejobs.DurableJobRegistry;
import durablejobs.DurableJobRunner;
import durablejobs.RecurringJobSchedulers;
import durablejobs.jobs.OldJobCleanupJob;
import java.time.Duration;
import javax.inject.Provider;
import repository.PersistedDurableJobRepository;
import scala.concurrent.ExecutionContext;

/**
 * Configures {@link durablejobs.DurableJob}s with their {@link DurableJobName} and, if they are
 * recurring, their {@link durablejobs.RecurringJobExecutionTimeResolver}.
 *
 * <p>NOTE: THIS SYSTEM IS STILL UNDER DEVELOPMENT AND THIS MODULE IS NOT CURRENTLY ENABLED IN
 * application.conf TODO(https://github.com/civiform/civiform/issues/4191): Enable DurableJobModule
 */
public final class DurableJobModule extends AbstractModule {

  @Override
  protected void configure() {
    // Binding the scheduler class as an eager singleton runs the constructor
    // at server start time.
    bind(DurableJobRunnerScheduler.class).asEagerSingleton();
  }

  /** Schedules the job runner to run on an interval using the akka scheduling system. */
  public static final class DurableJobRunnerScheduler {

    @Inject
    public DurableJobRunnerScheduler(
        ActorSystem actorSystem,
        Config config,
        ExecutionContext executionContext,
        Provider<DurableJobRunner> durableJobRunnerProvider) {
      int pollIntervalSeconds = config.getInt("durable_jobs.poll_interval_seconds");

      actorSystem
          .scheduler()
          .scheduleAtFixedRate(
              // Wait 30 seconds to give the server time to stabilize after start.
              /* initialDelay= */ Duration.ofSeconds(30),
              /* interval= */ Duration.ofSeconds(pollIntervalSeconds),
              () -> durableJobRunnerProvider.get().runJobs(),
              executionContext);
    }
  }

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
