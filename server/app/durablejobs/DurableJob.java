package durablejobs;

import models.PersistedDurableJobModel;

/**
 * Represents code that runs in a background thread. In contrast to org.apache.pekko tasks, they are
 * backed by {@link PersistedDurableJobModel} records in the database, making them durable to server
 * failures and allowing automated retry logic. The behavior of a {@code DurableJob} is determined
 * by its implementing class, which is identified by its associated {@link PersistedDurableJobModel}
 * record via the {@code jobName} attribute.
 */
public abstract class DurableJob {

  /** The {@link PersistedDurableJobModel} associated with this instance. */
  public abstract PersistedDurableJobModel getPersistedDurableJob();

  /** The jobName which identifies which type of job this is. */
  public String jobName() {
    return getPersistedDurableJob().getJobName();
  }

  /** Executes the job. */
  public abstract void run();
}
