package jobs;

import com.google.common.base.Preconditions;
import models.PersistedDurableJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repository.PersistedDurableJobRepository;

public final class OldJobCleanupJob implements DurableJob {
  Logger logger = LoggerFactory.getLogger(OldJobCleanupJob.class);

  private final PersistedDurableJobRepository persistedDurableJobRepository;
  private final PersistedDurableJob persistedDurableJob;

  public OldJobCleanupJob(
      PersistedDurableJobRepository persistedDurableJobRepository,
      PersistedDurableJob persistedDurableJob) {
    this.persistedDurableJobRepository = Preconditions.checkNotNull(persistedDurableJobRepository);
    this.persistedDurableJob = Preconditions.checkNotNull(persistedDurableJob);
  }

  @Override
  public PersistedDurableJob getPersistedDurableJob() {
    return persistedDurableJob;
  }

  @Override
  public void run() {
    int numRowsDeleted = this.persistedDurableJobRepository.deleteJobsOlderThanSixMonths();
    logger.info("Deleted {} old jobs", numRowsDeleted);
  }
}
