package jobs;

import models.PersistedDurableJob;

@FunctionalInterface
public interface DurableJobFactory {

  DurableJob create(PersistedDurableJob persistedDurableJob);
}
