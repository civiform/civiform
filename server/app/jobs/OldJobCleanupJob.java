package jobs;

import com.google.common.base.Preconditions;
import models.PersistedDurableJob;

public final class OldJobCleanupJob implements DurableJob {

  private final PersistedDurableJob persistedDurableJob;

  public OldJobCleanupJob(PersistedDurableJob persistedDurableJob) {
    this.persistedDurableJob = Preconditions.checkNotNull(persistedDurableJob);
  }

  @Override
  public PersistedDurableJob getPersistedDurableJob() {
    return persistedDurableJob;
  }

  @Override
  public void run() {}
}
