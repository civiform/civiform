package durablejobs.jobs;

import com.google.common.base.Preconditions;
import durablejobs.DurableJob;
import models.PersistedDurableJobModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repository.PersistedDurableJobRepository;

/**
 * A {@link DurableJob} that deletes {@link PersistedDurableJobModel} records from the database when
 * their execution time is older than six months.
 */
public final class OldJobCleanupJob extends DurableJob {
  private static final Logger LOGGER = LoggerFactory.getLogger(OldJobCleanupJob.class);

  private final PersistedDurableJobRepository persistedDurableJobRepository;
  private final PersistedDurableJobModel persistedDurableJob;

  public OldJobCleanupJob(
      PersistedDurableJobRepository persistedDurableJobRepository,
      PersistedDurableJobModel persistedDurableJob) {
    this.persistedDurableJobRepository = Preconditions.checkNotNull(persistedDurableJobRepository);
    this.persistedDurableJob = Preconditions.checkNotNull(persistedDurableJob);
  }

  @Override
  public PersistedDurableJobModel getPersistedDurableJob() {
    return persistedDurableJob;
  }

  @Override
  public void run() {
    int numRowsDeleted = this.persistedDurableJobRepository.deleteJobsOlderThanSixMonths();
    LOGGER.info("Deleted {} jobs older than 6 months", numRowsDeleted);
  }
}
