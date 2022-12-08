package repository;

import static org.assertj.core.api.Assertions.assertThat;

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
    Instant fiveMonthsAgo = Instant.now().minus(150, ChronoUnit.DAYS);
    var oneYearOldJob = new PersistedDurableJob("fake-name", fiveMonthsAgo);
    var sixMonthOldJob = new PersistedDurableJob("fake-name", oneYearAgo);
    oneYearOldJob.save();
    sixMonthOldJob.save();

    assertThat(repo.listJobs().size()).isEqualTo(2);
    repo.deleteJobsOlderThanSixMonths();
    assertThat(repo.listJobs().size()).isEqualTo(1);
  }
}
