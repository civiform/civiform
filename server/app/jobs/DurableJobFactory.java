package jobs;

import models.PersistedDurableJob;

/**
 * Implementations of {@code DurableJobFactory} produce instances of {@link DurableJob}. The main
 * reason for their existence is to provide an injection point for a {@link DurableJob}'s
 * dependencies.
 */
@FunctionalInterface
public interface DurableJobFactory {

  DurableJob create(PersistedDurableJob persistedDurableJob);
}
