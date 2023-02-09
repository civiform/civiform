package durablejobs;

import java.time.Clock;
import java.time.Instant;

/** Determines when a recurring {@link DurableJob} should next run. */
@FunctionalInterface
public interface RecurringJobExecutionTimeResolver {

  /**
   * Takes a {@link Clock} with the local time zone and returns an instant representing when the job
   * should next execute.
   */
  Instant resolveExecutionTime(Clock clock);
}
