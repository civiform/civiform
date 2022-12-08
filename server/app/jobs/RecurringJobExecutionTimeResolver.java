package jobs;

import java.time.Instant;

@FunctionalInterface
public interface RecurringJobExecutionTimeResolver {

  /**
   * Takes an instant representing the current time and returns an instant representing when the job
   * should execute.
   */
  Instant resolveExecutionTime(Instant now);
}
