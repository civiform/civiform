package jobs;

import models.PersistedDurableJob;

/**
 * {@code DurableJob}s are scheduled work that run in a background thread. In contrast to Akka
 * tasks, they are backed by {@link PersistedDurableJob} records in the database, making them
 * durable to server failures and allowing automated retry logic. The behavior of a {@code
 * DurableJob} is determined by its implementing class, which is identified by its associated {@link
 * PersistedDurableJob} record via the {@code jobName} attribute.
 */
public abstract class DurableJob {

  /** Get the {@code PersistedDurableJob} associated with this instance. */
  abstract PersistedDurableJob getPersistedDurableJob();

  /** Get the {@code jobName} which identifies which type of job this is. */
  public String jobName() {
    return getPersistedDurableJob().getJobName();
  }

  /** Execute the job. */
  abstract void run();
}
