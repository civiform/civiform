package jobs;

import java.time.Clock;
import java.time.Instant;

@FunctionalInterface
public interface RecurringJobExecutionTimeResolver {

  /**
   * Takes an instant representing the current time and returns an instant representing when the job
   * should execute.
   */
  Instant resolveExecutionTime(Clock clock);
}
