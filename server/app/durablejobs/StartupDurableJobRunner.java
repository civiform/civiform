package durablejobs;

import annotations.BindingAnnotations;
import com.google.common.base.Preconditions;
import com.typesafe.config.Config;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import models.PersistedDurableJobModel;
import repository.PersistedDurableJobRepository;
import services.cloud.aws.SimpleEmail;

/**
 * Executes {@link DurableJob}s when their time has come.
 *
 * <p>DurableJobRunner is a singleton and its {@code runJobs} method is {@code synchronized} to
 * prevent overlapping executions within the same server at the same time.
 */
@Singleton
public final class StartupDurableJobRunner extends AbstractDurableJobRunner {
  private final PersistedDurableJobRepository persistedDurableJobRepository;

  @Inject
  public StartupDurableJobRunner(
      Config config,
      DurableJobExecutionContext durableJobExecutionContext,
      @BindingAnnotations.StartupJobsProviderName DurableJobRegistry durableJobRegistry,
      PersistedDurableJobRepository persistedDurableJobRepository,
      @BindingAnnotations.Now Provider<LocalDateTime> nowProvider,
      SimpleEmail simpleEmail,
      ZoneId zoneId) {
    super(config, durableJobExecutionContext, durableJobRegistry, nowProvider, simpleEmail, zoneId);
    this.persistedDurableJobRepository = Preconditions.checkNotNull(persistedDurableJobRepository);
  }

  /** Get the job to run or an empty optional if one does not exist */
  @Override
  protected synchronized Optional<PersistedDurableJobModel> getJobForExecution() {
    return persistedDurableJobRepository.getStartupJobForExecution();
  }

  /** Determines if the provided job, if it exists, is allowed to be run. */
  @Override
  protected synchronized boolean canRun(Optional<PersistedDurableJobModel> maybeJobToRun) {
    return maybeJobToRun.isPresent();
  }
}
