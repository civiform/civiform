package durablejobs;

import com.google.inject.Inject;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class ExecutionTimeResolver {

  private final LocalDate date;

  @Inject
  public ExecutionTimeResolver(LocalDate date) {
    this.date = date;
  }

  /**
   * Computes the next execution time for a recurring job based on the local time settings
   * represented by {@code clock}.
   */
  public Instant resolveExecutionTime(Clock clock) {
    if (date != null) {
      return date.atStartOfDay(clock.getZone()).toInstant();
    } else {
      return LocalDate.now(clock)
          .plusDays(1)
          .atStartOfDay(clock.getZone())
          .plus(2, ChronoUnit.HOURS)
          .toInstant();
    }
  }
}
