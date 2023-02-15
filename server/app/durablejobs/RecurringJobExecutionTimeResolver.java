package durablejobs;

import java.time.Clock;
import java.time.Instant;

/** Determines when a recurring {@link DurableJob} should next run. */
@FunctionalInterface
public interface RecurringJobExecutionTimeResolver {

  /**
   * Computes the next execution time for a recurring job based on the local time settings
   * represented by {@code clock}.
   */
  Instant resolveExecutionTime(Clock clock);
}
