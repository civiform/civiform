package durablejobs;

import static org.assertj.core.api.Assertions.assertThat;

import annotations.BindingAnnotations;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;

import models.PersistedDurableJob;
import org.junit.Before;
import org.junit.Test;
import play.api.inject.BindingKey;
import play.api.inject.QualifierAnnotation;
import repository.PersistedDurableJobRepository;
import repository.ResetPostgres;

public class DurableJobRunnerTest extends ResetPostgres {

  private DurableJobRunner durableJobRunner;
  private DurableJobRegistry durableJobRegistry;
  private LocalDateTime now;
  private ZoneId zoneId;

  @Before
  public void setUp() {
    zoneId = instanceOf(ZoneId.class);
    now = instanceOf(new BindingKey<>(LocalDateTime.class).qualifiedWith(BindingAnnotations.Now.class));
    Config config =
        ConfigFactory.parseMap(
            ImmutableMap.of(
                "durable_jobs.job_timeout_minutes", 1,
                "durable_jobs.poll_interval_seconds", 0));

    durableJobRegistry = new DurableJobRegistry();

    durableJobRunner =
        new DurableJobRunner(
            config,
            durableJobRegistry,
            instanceOf(PersistedDurableJobRepository.class),
            () -> now,
            zoneId);
  }

  @Test
  public void runJobs_runsAJobThatIsReady() {
    AtomicInteger runCount = new AtomicInteger(0);
    durableJobRegistry.register(
        DurableJobName.TEST,
        (persistedDurableJob) -> makeTestJob(persistedDurableJob, () -> runCount.getAndIncrement()));

    createPersistedJobToExecute();
    createPersistedJobToExecute();
    createPersistedJobScheduledInFuture();

    durableJobRunner.runJobs();

    assertThat(runCount).hasValue(2);
  }

  private PersistedDurableJob createPersistedJobScheduledInFuture() {
    var persistedJob =
      new PersistedDurableJob(
        DurableJobName.TEST.getJobName(),
        now.plus(1, ChronoUnit.MONTHS).toInstant(zoneId.getRules().getOffset(now)));

    persistedJob.save();

    return persistedJob;
  }

  private PersistedDurableJob createPersistedJobToExecute() {
    var persistedJob =
      new PersistedDurableJob(
        DurableJobName.TEST.getJobName(),
        now.minus(1, ChronoUnit.SECONDS).toInstant(zoneId.getRules().getOffset(now)));

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
