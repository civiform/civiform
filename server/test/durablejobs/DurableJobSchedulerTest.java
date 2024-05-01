package durablejobs;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import models.PersistedDurableJobModel;
import org.junit.Before;
import org.junit.Test;
import repository.PersistedDurableJobRepository;
import repository.ResetPostgres;

public class DurableJobSchedulerTest extends ResetPostgres {

  private Clock clock;
  private DurableJobRegistry durableJobRegistry;
  private PersistedDurableJobRepository persistedDurableJobRepository;
  private RecurringJobScheduler recurringJobScheduler;

  @Before
  public void setUp() {
    clock = instanceOf(Clock.class);
    durableJobRegistry = new DurableJobRegistry();
    durableJobRegistry.register(
        DurableJobName.TEST,
        (persistedDurableJob) -> makeTestJob(persistedDurableJob, () -> {}),
        new RecurringJobExecutionTimeResolvers.Sunday2Am());

    persistedDurableJobRepository = instanceOf(PersistedDurableJobRepository.class);
    recurringJobScheduler =
        new RecurringJobScheduler(clock, durableJobRegistry, persistedDurableJobRepository);
  }

  @Test
  public void scheduleRecurringJobs_schedulesUnscheduledRecurringJobs() {
    var jobs = persistedDurableJobRepository.getJobs();
    assertThat(jobs.size()).isZero();

    recurringJobScheduler.scheduleRecurringJobs();

    jobs = persistedDurableJobRepository.getJobs();
    assertThat(jobs.size()).isEqualTo(1);
    var job = jobs.get(0);
    Instant expectedExecutionTime =
        new RecurringJobExecutionTimeResolvers.Sunday2Am().resolveExecutionTime(clock);
    assertThat(job.getExecutionTime()).isEqualTo(expectedExecutionTime);
    assertThat(job.getJobName()).isEqualTo(DurableJobName.TEST.getJobNameString());

    // Assert that subsequent runs do not create duplicates
    recurringJobScheduler.scheduleRecurringJobs();
    jobs = persistedDurableJobRepository.getJobs();
    assertThat(jobs.size()).isEqualTo(1);
    assertThat(job.getExecutionTime()).isEqualTo(expectedExecutionTime);
    assertThat(job.getJobName()).isEqualTo(DurableJobName.TEST.getJobNameString());
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
