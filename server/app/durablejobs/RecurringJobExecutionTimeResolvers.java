package durablejobs;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

/**
 * Holds implementations of {@link RecurringJobExecutionTimeResolver}. A {@link DurableJob} is a
 * recurring job if it is registered with a {@link RecurringJobExecutionTimeResolver}.
 *
 * <p>All implementations of {@link RecurringJobExecutionTimeResolver} MUST use the parameter {@link
 * Clock} for resolving execution times to ensure the local time zone is accounted for.
 */
public final class RecurringJobExecutionTimeResolvers {

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

  /** First day of the month at 2am local time. */
  public static final class FirstOfMonth2Am implements RecurringJobExecutionTimeResolver {

    @Override
    public Instant resolveExecutionTime(Clock clock) {
      return LocalDate.now(clock)
          .with(TemporalAdjusters.firstDayOfNextMonth())
          .atStartOfDay(clock.getZone())
          .plus(2, ChronoUnit.HOURS)
          .toInstant();
    }
  }

  /** Second day of the month at 2am local time. */
  public static final class SecondOfMonth2Am implements RecurringJobExecutionTimeResolver {

    @Override
    public Instant resolveExecutionTime(Clock clock) {
      return LocalDate.now(clock)
          .with(TemporalAdjusters.firstDayOfNextMonth())
          .plusDays(1L)
          .atStartOfDay(clock.getZone())
          .plus(2, ChronoUnit.HOURS)
          .toInstant();
    }
  }

  /** Third day of the month at 2am local time. */
  public static final class ThirdOfMonth2Am implements RecurringJobExecutionTimeResolver {

    @Override
    public Instant resolveExecutionTime(Clock clock) {
      return LocalDate.now(clock)
          .with(TemporalAdjusters.firstDayOfNextMonth())
          .plusDays(2L)
          .atStartOfDay(clock.getZone())
          .plus(2, ChronoUnit.HOURS)
          .toInstant();
    }
  }

  /** Nightly at 2am local time */
  public static final class Nightly2Am implements RecurringJobExecutionTimeResolver {

    @Override
    public Instant resolveExecutionTime(Clock clock) {
      return LocalDate.now(clock)
        .plusDays(1)
          .atStartOfDay(clock.getZone())
          .plus(2, ChronoUnit.HOURS)
          .toInstant();
    }
  }

  public static final class Immediately implements RecurringJobExecutionTimeResolver {
    @Override
    public Instant resolveExecutionTime(Clock clock) {
      return LocalDateTime.now(clock)
          .toInstant(ZoneId.systemDefault().getRules().getOffset(Instant.now()));
    }
  }
}
