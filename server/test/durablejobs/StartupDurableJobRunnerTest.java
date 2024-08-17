package durablejobs;

import static org.assertj.core.api.Assertions.assertThat;

import annotations.BindingAnnotations;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.ebean.DB;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import models.JobType;
import models.PersistedDurableJobModel;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import play.api.inject.BindingKey;
import repository.PersistedDurableJobRepository;
import repository.ResetPostgres;
import services.cloud.aws.SimpleEmail;
import support.TestRetry;

public class StartupDurableJobRunnerTest extends ResetPostgres {

  @Rule public TestRetry testRetry = new TestRetry(5);

  private SimpleEmail simpleEmailMock;
  private StartupDurableJobRunner recurringDurableJobRunner;
  private DurableJobRegistry durableJobRegistry;

  @Before
  public void setUp() {
    simpleEmailMock = Mockito.mock(SimpleEmail.class);

    Config config =
        ConfigFactory.parseMap(
            ImmutableMap.of(
                "it_email_address",
                "test@example.com",
                "base_url",
                "https://civiform-test.dev",
                "durable_jobs.job_timeout_minutes",
                0,
                "durable_jobs.poll_interval_seconds",
                0));

    durableJobRegistry = new DurableJobRegistry();

    recurringDurableJobRunner =
        new StartupDurableJobRunner(
            config,
            instanceOf(DurableJobExecutionContext.class),
            durableJobRegistry,
            instanceOf(PersistedDurableJobRepository.class),
            () ->
                instanceOf(
                    new BindingKey<>(LocalDateTime.class)
                        .qualifiedWith(BindingAnnotations.Now.class)),
            simpleEmailMock,
            instanceOf(ZoneId.class));
  }

  @Test
  public void runJobs_timesOut() {
    durableJobRegistry.registerWithNoTimeResolver(
        DurableJobName.TEST,
        JobType.RUN_ONCE,
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

    PersistedDurableJobModel job = createPersistedJobToExecute();

    recurringDurableJobRunner.runJobs();

    job.refresh();
    assertThat(job.getErrorMessage().get()).contains("JobRunner_JobTimeout");
  }

  @Test
  public void rubJobs_executionException() {
    durableJobRegistry.registerWithNoTimeResolver(
        DurableJobName.TEST,
        JobType.RUN_ONCE,
        (persistedDurableJob) ->
            makeTestJob(
                persistedDurableJob,
                () -> {
                  throw new RuntimeException("test-execution-exception");
                }));

    PersistedDurableJobModel job = createPersistedJobToExecute();

    recurringDurableJobRunner.runJobs();

    job.refresh();

    assertThat(job.getErrorMessage().get()).contains("JobRunner_JobFailed");
    assertThat(job.getRemainingAttempts()).isEqualTo(0);
  }

  @Test
  public void runJobs_runsJobsThatAreReady() {
    AtomicInteger runCount = new AtomicInteger(0);
    durableJobRegistry.registerWithNoTimeResolver(
        DurableJobName.TEST,
        JobType.RUN_ONCE,
        (persistedDurableJob) ->
            makeTestJob(persistedDurableJob, () -> runCount.getAndIncrement()));

    PersistedDurableJobModel jobA = createPersistedJobToExecute();
    PersistedDurableJobModel jobB = createPersistedJobToExecute();
    PersistedDurableJobModel jobC = createPersistedJobScheduledInFuture();

    recurringDurableJobRunner.runJobs();

    jobA.refresh();
    jobB.refresh();
    jobC.refresh();

    // This assertion fails occasionally. I've been unable to figure out why
    // so added RetryTest rule - bionj@google.com 5/18/2023.
    assertThat(runCount).hasValue(3);

    assertThat(jobA.getRemainingAttempts()).isEqualTo(2);
    assertThat(jobB.getRemainingAttempts()).isEqualTo(2);
    assertThat(jobC.getRemainingAttempts()).isEqualTo(2);

    assertThat(jobA.getSuccessTime()).isPresent();
    assertThat(jobB.getSuccessTime()).isPresent();
    assertThat(jobC.getSuccessTime()).isPresent();

    Mockito.verifyNoInteractions(simpleEmailMock);
  }

  @Test
  public void runJobs_jobNotFound_deletesJobFromDb() {
    PersistedDurableJobModel job = createPersistedJobToExecute();

    // Assert the job was saved to the db
    Optional<PersistedDurableJobModel> savedJob =
        DB.getDefault()
            .find(PersistedDurableJobModel.class)
            .where()
            .eq("jobName", job.getJobName())
            .findOneOrEmpty();
    assertThat(savedJob.get().getJobName()).isEqualTo(job.getJobName());

    // Assert the job does not exist in the registry
    assertThat(durableJobRegistry.getRecurringJobs()).isEmpty();

    // Since the job does not exist in the registry, it should be deleted when runJobs is run
    recurringDurableJobRunner.runJobs();
    Optional<PersistedDurableJobModel> foundJob =
        DB.getDefault()
            .find(PersistedDurableJobModel.class)
            .where()
            .eq("jobName", job.getJobName())
            .findOneOrEmpty();
    assertThat(foundJob).isEmpty();
  }

  private PersistedDurableJobModel createPersistedJobScheduledInFuture() {
    var persistedJob =
        new PersistedDurableJobModel(
            DurableJobName.TEST.getJobNameString(),
            JobType.RUN_ONCE,
            Instant.now().plus(10, ChronoUnit.DAYS));

    persistedJob.save();

    return persistedJob;
  }

  private PersistedDurableJobModel createPersistedJobToExecute() {
    var persistedJob =
        new PersistedDurableJobModel(
            DurableJobName.TEST.getJobNameString(),
            JobType.RUN_ONCE,
            Instant.now().minus(1, ChronoUnit.DAYS));

    persistedJob.save();

    return persistedJob;
  }

  private static DurableJob makeTestJob(
      PersistedDurableJobModel persistedDurableJob, Runnable runnable) {
    return new DurableJob() {
      @Override
      public PersistedDurableJobModel getPersistedDurableJob() {
        return persistedDurableJob;
      }

      @Override
      public void run() {
        runnable.run();
      }
    };
  }
}
