package repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import models.PersistedDurableJob;
import org.junit.Before;
import org.junit.Test;

public class PersistedDurableJobRepositoryTest extends ResetPostgres {
  private PersistedDurableJobRepository repo;

  @Before
  public void setUp() {
    repo = instanceOf(PersistedDurableJobRepository.class);
  }

  @Test
  public void deleteJobsOlderThanSixMonths() {
    Instant oneYearAgo = Instant.now().minus(365, ChronoUnit.DAYS);
    Instant fiveMonthsAgo = Instant.now().minus(5 * 30, ChronoUnit.DAYS);
    var oneYearOldJob = new PersistedDurableJob("fake-name", oneYearAgo);
    var fiveMonthOldJob = new PersistedDurableJob("fake-name", fiveMonthsAgo);
    oneYearOldJob.save();
    fiveMonthOldJob.save();

    assertThat(repo.getJobs().size()).isEqualTo(2);
    repo.deleteJobsOlderThanSixMonths();
    ImmutableList<PersistedDurableJob> remainingJobs = repo.getJobs();
    assertThat(remainingJobs.size()).isEqualTo(1);
    assertThat(remainingJobs.get(0)).isEqualTo(fiveMonthOldJob);
  }
}
