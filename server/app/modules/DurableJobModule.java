package modules;

import akka.actor.ActorSystem;
import annotations.BindingAnnotations;
import annotations.BindingAnnotations.RecurringJobsProviderName;
import annotations.BindingAnnotations.StartupJobsProviderName;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.typesafe.config.Config;
import durablejobs.DurableJobName;
import durablejobs.DurableJobRegistry;
import durablejobs.RecurringDurableJobRunner;
import durablejobs.RecurringJobExecutionTimeResolvers;
import durablejobs.RecurringJobScheduler;
import durablejobs.StartupDurableJobRunner;
import durablejobs.StartupJobScheduler;
import durablejobs.jobs.MigratePrimaryApplicantInfoJob;
import durablejobs.jobs.OldJobCleanupJob;
import durablejobs.jobs.ReportingDashboardMonthlyRefreshJob;
import durablejobs.jobs.UnusedAccountCleanupJob;
import durablejobs.jobs.UnusedProgramImagesCleanupJob;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Random;
import models.JobType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.api.db.evolutions.ApplicationEvolutions;
import repository.AccountRepository;
import repository.PersistedDurableJobRepository;
import repository.ReportingRepository;
import repository.VersionRepository;
import scala.concurrent.ExecutionContext;
import services.cloud.PublicStorageClient;
import services.settings.SettingsService;

/**
 * Configures {@link durablejobs.DurableJob}s with their {@link DurableJobName} and, if they are
 * recurring, their {@link durablejobs.RecurringJobExecutionTimeResolver}.
 */
public final class DurableJobModule extends AbstractModule {
  private static final Logger LOGGER = LoggerFactory.getLogger(DurableJobModule.class);

  @Override
  protected void configure() {
    // Binding the scheduler class as an eager singleton runs the constructor
    // at server start time.
    LOGGER.trace("Module Started");
    bind(DurableJobRunnerScheduler.class).asEagerSingleton();
  }

  /**
   * This class injects ApplicationEvolutions and checks the `upToDate` method to prevent this
   * module from running until after the evolutions are completed.
   *
   * <p>See <a href="https://github.com/civiform/civiform/pull/8253">PR 8253</a> for more extensive
   * details.
   *
   * <p>Additionally this uses The Akka scheduling system to schedules the job runner to run on an
   * interval.
   */
  public static final class DurableJobRunnerScheduler {

    @Inject
    public DurableJobRunnerScheduler(
        ApplicationEvolutions applicationEvolutions,
        ActorSystem actorSystem,
        Config config,
        ExecutionContext executionContext,
        RecurringDurableJobRunner recurringDurableJobRunner,
        RecurringJobScheduler recurringJobScheduler,
        StartupDurableJobRunner startupDurableJobRunner,
        StartupJobScheduler startupJobScheduler) {
      LOGGER.trace("DurableJobRunnerScheduler - Started");
      int pollIntervalSeconds = config.getInt("durable_jobs.poll_interval_seconds");

      if (applicationEvolutions.upToDate()) {
        LOGGER.trace("DurableJobRunnerScheduler - Task Start");

        // Run startup jobs. These jobs must complete before the application can start serving
        // pages.
        startupJobScheduler.scheduleRecurringJobs();
        startupDurableJobRunner.runJobs();

        // Start the actorSystem to run recurring jobs. These jobs will run in the background after
        // the configured initial delay
        actorSystem
            .scheduler()
            .scheduleAtFixedRate(
                // Wait a random amount of time to decrease likelihood of synchronized
                // polling with another server.
                /* initialDelay= */ Duration.ofSeconds(new Random().nextInt(/* bound= */ 30)),
                /* interval= */ Duration.ofSeconds(pollIntervalSeconds),
                () -> {
                  recurringJobScheduler.scheduleRecurringJobs();
                  recurringDurableJobRunner.runJobs();
                },
                executionContext);
        LOGGER.trace("DurableJobRunnerScheduler - Task End");
      } else {
        LOGGER.trace("Evolutions Not Ready");
      }
    }
  }

  @Provides
  @RecurringJobsProviderName
  public DurableJobRegistry provideRecurringDurableJobRegistry(
      AccountRepository accountRepository,
      @BindingAnnotations.Now Provider<LocalDateTime> nowProvider,
      PersistedDurableJobRepository persistedDurableJobRepository,
      PublicStorageClient publicStorageClient,
      ReportingRepository reportingRepository,
      VersionRepository versionRepository,
      SettingsService settingsService,
      Config config) {
    var durableJobRegistry = new DurableJobRegistry();

    durableJobRegistry.register(
        DurableJobName.OLD_JOB_CLEANUP,
        JobType.RECURRING,
        persistedDurableJob ->
            new OldJobCleanupJob(persistedDurableJobRepository, persistedDurableJob),
        new RecurringJobExecutionTimeResolvers.Sunday2Am());

    durableJobRegistry.register(
        DurableJobName.REPORTING_DASHBOARD_MONTHLY_REFRESH,
        JobType.RECURRING,
        persistedDurableJob ->
            new ReportingDashboardMonthlyRefreshJob(reportingRepository, persistedDurableJob),
        new RecurringJobExecutionTimeResolvers.FirstOfMonth2Am());

    durableJobRegistry.register(
        DurableJobName.UNUSED_ACCOUNT_CLEANUP,
        JobType.RECURRING,
        persistedDurableJob ->
            new UnusedAccountCleanupJob(accountRepository, nowProvider, persistedDurableJob),
        new RecurringJobExecutionTimeResolvers.SecondOfMonth2Am());

    durableJobRegistry.register(
        DurableJobName.UNUSED_PROGRAM_IMAGES_CLEANUP,
        JobType.RECURRING,
        persistedDurableJob ->
            new UnusedProgramImagesCleanupJob(
                publicStorageClient, versionRepository, persistedDurableJob),
        new RecurringJobExecutionTimeResolvers.ThirdOfMonth2Am());

    durableJobRegistry.register(
        DurableJobName.MIGRATE_PRIMARY_APPLICANT_INFO,
        JobType.RECURRING,
        persistedDurableJob ->
            new MigratePrimaryApplicantInfoJob(
                persistedDurableJob, accountRepository, settingsService, config),
        new RecurringJobExecutionTimeResolvers.Nightly3Am());

    return durableJobRegistry;
  }

  @Provides
  @StartupJobsProviderName
  public DurableJobRegistry provideStartupDurableJobRegistry(
      PersistedDurableJobRepository persistedDurableJobRepository) {
    var durableJobRegistry = new DurableJobRegistry();

    durableJobRegistry.register(
        DurableJobName.TEST,
        JobType.RUN_ON_EACH_STARTUP,
        persistedDurableJob ->
            new OldJobCleanupJob(persistedDurableJobRepository, persistedDurableJob));

    return durableJobRegistry;
  }
}
