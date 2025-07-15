package durablejobs;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;

/**
 * Holds implementations of {@link JobExecutionTimeResolver}. A {@link DurableJob} is a recurring
 * job if it is registered with a {@link JobExecutionTimeResolver}.
 *
 * <p>All implementations of {@link JobExecutionTimeResolver} MUST use the parameter {@link Clock}
 * for resolving execution times to ensure the local time zone is accounted for.
 */
public final class RecurringJobExecutionTimeResolvers {

  /** Every Sunday at 2am local time. Used for the OLD_JOB_CLEANUP job. */
  public static final class Sunday2Am implements JobExecutionTimeResolver {

    @Override
    public Instant resolveExecutionTime(Clock clock) {
      return LocalDate.now(clock)
          .with(TemporalAdjusters.next(DayOfWeek.SUNDAY))
          .atStartOfDay(clock.getZone())
          .plus(2, ChronoUnit.HOURS)
          .toInstant();
    }
  }

  /**
   * First day of the month at 2am local time. Used for the REPORTING_DASHBOARD_MONTHLY_REFRESH job.
   */
  public static final class FirstOfMonth2Am implements JobExecutionTimeResolver {

    @Override
    public Instant resolveExecutionTime(Clock clock) {
      return LocalDate.now(clock)
          .with(TemporalAdjusters.firstDayOfNextMonth())
          .atStartOfDay(clock.getZone())
          .plus(2, ChronoUnit.HOURS)
          .toInstant();
    }
  }

  /** Second day of the month at 2am local time. Used for the UNUSED_ACCOUNT_CLEANUP job. */
  public static final class SecondOfMonth2Am implements JobExecutionTimeResolver {

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

  /** Third day of the month at 2am local time. Used for the UNUSED_PROGRAM_IMAGES_CLEANUP job. */
  public static final class ThirdOfMonth2Am implements JobExecutionTimeResolver {

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

  /** Admin configured interval for durable jobs. Used for REFRESH_MAP_DATA */
  public static final class AdminConfiguredResolver implements JobExecutionTimeResolver {
    private final Integer refreshInterval;

    public AdminConfiguredResolver(Optional<Integer> refreshInterval) {
      this.refreshInterval = refreshInterval.orElse(30);
    }

    @Override
    public Instant resolveExecutionTime(Clock clock) {
      LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), clock.getZone());
      return now
          .plusMinutes(minutesUntilNextInterval(now))
          .withSecond(0)
          .withNano(0)
          .atZone(clock.getZone())
          .toInstant();
    }

    private int minutesUntilNextInterval(LocalDateTime now) {
      return this.refreshInterval - (now.getMinute() % this.refreshInterval);
    }
  }
}
