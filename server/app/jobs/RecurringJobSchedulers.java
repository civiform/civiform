package jobs;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

public final class RecurringJobSchedulers {

  /** Every Sunday at 2am local time. */
  public static Instant everySundayAt2Am(Clock clock) {
    return LocalDate.now(clock)
        .with(TemporalAdjusters.next(DayOfWeek.SUNDAY))
        .atStartOfDay(clock.getZone())
        .plus(2, ChronoUnit.HOURS)
        .toInstant();
  }
}
