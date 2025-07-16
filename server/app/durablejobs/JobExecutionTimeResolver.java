package durablejobs;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

/** Determines when a recurring {@link DurableJob} should next run. */
@FunctionalInterface
public interface JobExecutionTimeResolver {

  /**
   * Computes the next execution time for a recurring job based on the local time settings
   * represented by {@code clock}.
   */
  Instant resolveExecutionTime(Clock clock);
}
