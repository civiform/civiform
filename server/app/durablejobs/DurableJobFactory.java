package durablejobs;

import models.PersistedDurableJobModel;

/**
 * Defines factories that produce instances of {@link DurableJob}.
 *
 * <p>The main reason for their existence is to provide an injection point for a {@link
 * DurableJob}'s dependencies.
 */
@FunctionalInterface
public interface DurableJobFactory {

  DurableJob create(PersistedDurableJobModel persistedDurableJob);
}
