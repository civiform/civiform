package durablejobs;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import models.JobType;
import models.PersistedDurableJobModel;
import org.junit.Before;
import org.junit.Test;
import repository.PersistedDurableJobRepository;
import repository.ResetPostgres;

public class StartupJobSchedulerTest extends ResetPostgres {

  private Clock clock;
  private DurableJobRegistry durableJobRegistry;
  private PersistedDurableJobRepository persistedDurableJobRepository;
  private StartupJobScheduler startupJobScheduler;

  @Before
  public void setUp() {
    clock = instanceOf(Clock.class);
    durableJobRegistry = new DurableJobRegistry();
    durableJobRegistry.registerStartupJob(
        DurableJobName.TEST,
        JobType.RUN_ONCE,
        (persistedDurableJob) -> makeTestJob(persistedDurableJob, () -> {}));

    persistedDurableJobRepository = instanceOf(PersistedDurableJobRepository.class);
    startupJobScheduler =
        new StartupJobScheduler(clock, durableJobRegistry, persistedDurableJobRepository);
  }

  @Test
  public void scheduleStartupJobs_schedulesUnscheduledStartupJobs() {
    var jobs = persistedDurableJobRepository.getJobs();
    assertThat(jobs.size()).isZero();

    startupJobScheduler.scheduleJobs();

    jobs = persistedDurableJobRepository.getJobs();
    assertThat(jobs.size()).isEqualTo(1);
    var job = jobs.get(0);
    assertThat(job.getJobName()).isEqualTo(DurableJobName.TEST.getJobNameString());

    // Assert that subsequent runs do not create duplicates
    startupJobScheduler.scheduleJobs();
    jobs = persistedDurableJobRepository.getJobs();
    assertThat(jobs.size()).isEqualTo(1);
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
