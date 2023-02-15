package durablejobs;

import static org.assertj.core.api.Assertions.assertThat;

import annotations.BindingAnnotations;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;
import models.PersistedDurableJob;
import org.junit.Before;
import org.junit.Test;
import play.api.inject.BindingKey;
import repository.PersistedDurableJobRepository;
import repository.ResetPostgres;

public class DurableJobRunnerTest extends ResetPostgres {

  private DurableJobRunner durableJobRunner;
  private DurableJobRegistry durableJobRegistry;

  @Before
  public void setUp() {
    Config config =
        ConfigFactory.parseMap(
            ImmutableMap.of(
                "durable_jobs.job_timeout_minutes", 0,
                "durable_jobs.poll_interval_seconds", 0));

    durableJobRegistry = new DurableJobRegistry();

    durableJobRunner =
        new DurableJobRunner(
            config,
            instanceOf(DurableJobExecutionContext.class),
            durableJobRegistry,
            instanceOf(PersistedDurableJobRepository.class),
            () ->
                instanceOf(
                    new BindingKey<>(LocalDateTime.class)
                        .qualifiedWith(BindingAnnotations.Now.class)),
            instanceOf(ZoneId.class));
  }

  @Test
  public void runJobs_timesOut() {
    durableJobRegistry.register(
        DurableJobName.TEST,
        (persistedDurableJob) ->
            makeTestJob(
                persistedDurableJob,
                () -> {
                  try {
                    Thread.sleep(/* millis= */ 3000L);
                  } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                  }
                }));

    PersistedDurableJob job = createPersistedJobToExecute();

    durableJobRunner.runJobs();

    job.refresh();
    assertThat(job.getErrorMessage().get()).contains("JobRunner_JobTimeout");
  }

  @Test
  public void rubJobs_executionException() {
    durableJobRegistry.register(
        DurableJobName.TEST,
        (persistedDurableJob) ->
            makeTestJob(
                persistedDurableJob,
                () -> {
                  throw new RuntimeException("test-execution-exception");
                }));

    PersistedDurableJob job = createPersistedJobToExecute();

    durableJobRunner.runJobs();

    job.refresh();

    assertThat(job.getErrorMessage().get()).contains("JobRunner_JobFailed");
    assertThat(job.getRemainingAttempts()).isEqualTo(0);
  }

  @Test
  public void runJobs_runsJobsThatAreReady() {
    AtomicInteger runCount = new AtomicInteger(0);
    durableJobRegistry.register(
        DurableJobName.TEST,
        (persistedDurableJob) ->
            makeTestJob(persistedDurableJob, () -> runCount.getAndIncrement()));

    PersistedDurableJob jobA = createPersistedJobToExecute();
    PersistedDurableJob jobB = createPersistedJobToExecute();
    PersistedDurableJob jobC = createPersistedJobScheduledInFuture();

    durableJobRunner.runJobs();

    jobA.refresh();
    jobB.refresh();
    jobC.refresh();

    assertThat(runCount).hasValue(2);

    assertThat(jobA.getRemainingAttempts()).isEqualTo(2);
    assertThat(jobB.getRemainingAttempts()).isEqualTo(2);
    assertThat(jobC.getRemainingAttempts()).isEqualTo(3);

    assertThat(jobA.getSuccessTime()).isPresent();
    assertThat(jobB.getSuccessTime()).isPresent();
    assertThat(jobC.getSuccessTime()).isEmpty();
  }

  @Test
  public void runJobs_jobNotFound() {
    PersistedDurableJob job = createPersistedJobToExecute();

    durableJobRunner.runJobs();

    job.refresh();
    assertThat(job.getErrorMessage().get()).contains("JobRunner_JobFailed JobNotFound");
    assertThat(job.getRemainingAttempts()).isEqualTo(0);
  }

  private PersistedDurableJob createPersistedJobScheduledInFuture() {
    var persistedJob =
        new PersistedDurableJob(
            DurableJobName.TEST.getJobName(), Instant.now().plus(10, ChronoUnit.DAYS));

    persistedJob.save();

    return persistedJob;
  }

  private PersistedDurableJob createPersistedJobToExecute() {
    var persistedJob =
        new PersistedDurableJob(
            DurableJobName.TEST.getJobName(), Instant.now().minus(1, ChronoUnit.DAYS));

    persistedJob.save();

    return persistedJob;
  }

  private static DurableJob makeTestJob(
      PersistedDurableJob persistedDurableJob, Runnable runnable) {
    return new DurableJob() {
      @Override
      public PersistedDurableJob getPersistedDurableJob() {
        return persistedDurableJob;
      }

      @Override
      public void run() {
        runnable.run();
      }
    };
  }
}
