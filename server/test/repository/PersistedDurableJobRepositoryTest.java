package repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import io.ebean.DB;
import io.ebean.Database;
import io.ebean.Transaction;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import models.JobType;
import models.PersistedDurableJobModel;
import org.junit.Before;
import org.junit.Test;

public class PersistedDurableJobRepositoryTest extends ResetPostgres {
  private PersistedDurableJobRepository repo;
  private Database database;

  @Before
  public void setUp() {
    repo = instanceOf(PersistedDurableJobRepository.class);
    database = DB.getDefault();
  }

  @Test
  public void findScheduledJob_findsJobsAtATime() {
    Instant tomorrow = Instant.now().plus(1, ChronoUnit.DAYS);
    var job = new PersistedDurableJobModel("fake-name", JobType.RECURRING, tomorrow);

    assertThat(repo.findScheduledJob("fake-name", tomorrow)).isEmpty();
    job.save();
    assertThat(repo.findScheduledJob("fake-name", tomorrow)).contains(job);
  }

  @Test
  public void getJobForExecution_locksRowsForUpdateAndSkipsThem() throws Throwable {
    Instant yesterday = Instant.now().minus(1, ChronoUnit.DAYS);
    var jobA = new PersistedDurableJobModel("fake-name", JobType.RECURRING, yesterday);
    jobA.save();

    // Exceptions thrown in a separate thread won't cause the test to fail, so
    // we capture it in a ref and throw at the end of the test if it's present.
    AtomicReference<Optional<Throwable>> threadException = new AtomicReference<>(Optional.empty());

    try (Transaction transactionA = database.beginTransaction()) {
      Optional<PersistedDurableJobModel> firstFoundJob = repo.getJobForExecution();
      assertThat(firstFoundJob.get()).isEqualTo(jobA);

      // Because EBean transactions are thread-local we start a new thread so
      // that we can have two transactions open at the same time.
      Thread thread =
          new Thread(
              () -> {
                try (Transaction transactionB = database.beginTransaction()) {
                  // There is only one job in the table, and it is locked by
                  // a transaction in the parent thread, querying for jobs
                  // should therefore return empty.
                  Optional<PersistedDurableJobModel> secondFoundJob = repo.getJobForExecution();
                  assertThat(secondFoundJob).isEmpty();

                  // After saving the second job, it should now be available for
                  // execution.
                  var jobB =
                      new PersistedDurableJobModel("fake-name", JobType.RECURRING, yesterday);
                  jobB.save();
                  secondFoundJob = repo.getJobForExecution();
                  assertThat(secondFoundJob.get()).isEqualTo(jobB);
                }
              });
      thread.setUncaughtExceptionHandler(
          (t, exception) -> threadException.set(Optional.of(exception)));
      thread.start();
      thread.join();
    }

    if (threadException.get().isPresent()) {
      throw threadException.get().get();
    }
  }

  @Test
  public void deleteJobsOlderThanSixMonths() {
    Instant oneYearAgo = Instant.now().minus(365, ChronoUnit.DAYS);
    Instant fiveMonthsAgo = Instant.now().minus(5 * 30, ChronoUnit.DAYS);
    var oneYearOldJob = new PersistedDurableJobModel("fake-name", JobType.RECURRING, oneYearAgo);
    var fiveMonthOldJob =
        new PersistedDurableJobModel("fake-name", JobType.RECURRING, fiveMonthsAgo);
    oneYearOldJob.save();
    fiveMonthOldJob.save();

    assertThat(repo.getJobs().size()).isEqualTo(2);
    repo.deleteJobsOlderThanSixMonths();
    ImmutableList<PersistedDurableJobModel> remainingJobs = repo.getJobs();
    assertThat(remainingJobs.size()).isEqualTo(1);
    assertThat(remainingJobs.get(0)).isEqualTo(fiveMonthOldJob);
  }
}
