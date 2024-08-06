package modules;

import akka.actor.ActorSystem;
import annotations.BindingAnnotations;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.typesafe.config.Config;
import durablejobs.DurableJobName;
import durablejobs.DurableJobRegistry;
import durablejobs.DurableJobRunner;
import durablejobs.RecurringJobExecutionTimeResolvers;
import durablejobs.RecurringJobScheduler;
import durablejobs.jobs.MigratePrimaryApplicantInfoJob;
import durablejobs.jobs.OldJobCleanupJob;
import durablejobs.jobs.ReportingDashboardMonthlyRefreshJob;
import durablejobs.jobs.UnusedAccountCleanupJob;
import durablejobs.jobs.UnusedProgramImagesCleanupJob;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Random;
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
        ApplicationEvolutions applicationEvolutions,
        ActorSystem actorSystem,
        Config config,
        ExecutionContext executionContext,
        DurableJobRunner durableJobRunner,
        RecurringJobScheduler recurringJobScheduler) {
      int pollIntervalSeconds = config.getInt("durable_jobs.poll_interval_seconds");

      if (applicationEvolutions.upToDate()) {
        actorSystem
            .scheduler()
            .scheduleAtFixedRate(
                // Wait a random amount of time to decrease likelihood of synchronized
                // polling with another server.
                /* initialDelay= */ Duration.ofSeconds(new Random().nextInt(/* bound= */ 30)),
                /* interval= */ Duration.ofSeconds(pollIntervalSeconds),
                () -> {
                  recurringJobScheduler.scheduleRecurringJobs();
                  durableJobRunner.runJobs();
                },
                executionContext);
      }
    }
  }

  @Provides
  public DurableJobRegistry provideDurableJobRegistry(
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
        persistedDurableJob ->
            new OldJobCleanupJob(persistedDurableJobRepository, persistedDurableJob),
        new RecurringJobExecutionTimeResolvers.Sunday2Am());

    durableJobRegistry.register(
        DurableJobName.REPORTING_DASHBOARD_MONTHLY_REFRESH,
        persistedDurableJob ->
            new ReportingDashboardMonthlyRefreshJob(reportingRepository, persistedDurableJob),
        new RecurringJobExecutionTimeResolvers.FirstOfMonth2Am());

    durableJobRegistry.register(
        DurableJobName.UNUSED_ACCOUNT_CLEANUP,
        persistedDurableJob ->
            new UnusedAccountCleanupJob(accountRepository, nowProvider, persistedDurableJob),
        new RecurringJobExecutionTimeResolvers.SecondOfMonth2Am());

    durableJobRegistry.register(
        DurableJobName.UNUSED_PROGRAM_IMAGES_CLEANUP,
        persistedDurableJob ->
            new UnusedProgramImagesCleanupJob(
                publicStorageClient, versionRepository, persistedDurableJob),
        new RecurringJobExecutionTimeResolvers.ThirdOfMonth2Am());

    durableJobRegistry.register(
        DurableJobName.MIGRATE_PRIMARY_APPLICANT_INFO,
        persistedDurableJob ->
            new MigratePrimaryApplicantInfoJob(
                persistedDurableJob, accountRepository, settingsService, config),
        new RecurringJobExecutionTimeResolvers.Nightly3Am());

    return durableJobRegistry;
  }
}
