package durablejobs;

import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;

import io.ebean.DB;
import io.ebean.Database;
import io.ebean.Transaction;
import models.PersistedDurableJob;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import repository.PersistedDurableJobRepository;
import repository.ResetPostgres;

import javax.persistence.OptimisticLockException;

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

  @Test
  public void scheduleRecurringJobs_retriesFailedInsertions() {
    try (MockedStatic<DB> mockDb = mockStatic(DB.class)) {
      var mockTransactionA = mock(Transaction.class);
      var mockDatabase = mock(Database.class);
      mockDb.when(DB::getDefault).thenReturn(mockDatabase);
      when(mockDatabase.beginTransaction())
        .thenReturn(mockTransactionA)
        .thenAnswer((Answer<Transaction>) invocation -> DB.beginTransaction());

      doThrow(OptimisticLockException.class).when(mockTransactionA).commit();

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
    }
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
