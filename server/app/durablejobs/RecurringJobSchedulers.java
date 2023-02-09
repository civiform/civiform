package durablejobs;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

/**
 * Holds implementations of {@link RecurringJobExecutionTimeResolver}. A {@link DurableJob} is a
 * recurring job if it is registered with a {@link RecurringJobExecutionTimeResolver}.
 *
 * <p>All implementations of {@link RecurringJobExecutionTimeResolver} MUST use the parameter {@link
 * Clock} for resolving execution times to ensure the local time zone is accounted for.
 */
public final class RecurringJobSchedulers {

  /** Every Sunday at 2am local time. */
  public static final class Sunday2Am implements RecurringJobExecutionTimeResolver {

    @Override
    public Instant resolveExecutionTime(Clock clock) {
      return LocalDate.now(clock)
          .with(TemporalAdjusters.next(DayOfWeek.SUNDAY))
          .atStartOfDay(clock.getZone())
          .plus(2, ChronoUnit.HOURS)
          .toInstant();
    }
  }
}
