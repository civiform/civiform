package durablejobs;

import models.PersistedDurableJob;

/**
 * Represents code that runs in a background thread. In contrast to Akka tasks, they are backed by
 * {@link PersistedDurableJob} records in the database, making them durable to server failures and
 * allowing automated retry logic. The behavior of a {@code DurableJob} is determined by its
 * implementing class, which is identified by its associated {@link PersistedDurableJob} record via
 * the {@code jobName} attribute.
 */
public abstract class DurableJob {

  /** The {@link PersistedDurableJob} associated with this instance. */
  public abstract PersistedDurableJob getPersistedDurableJob();

  /** The jobName which identifies which type of job this is. */
  public String jobName() {
    return getPersistedDurableJob().getJobName();
  }

  /** Executes the job. */
  public abstract void run();
}
