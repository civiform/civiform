package jobs;

import models.PersistedDurableJob;

public interface DurableJob {

  /** Get the {@code PersistedDurableJob} associated with this instance. */
  PersistedDurableJob getPersistedDurableJob();

  /** Execute the job. */
  void run();
}
