package durablejobs;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;

/**
 * Holds implementations of {@link ExecutionTimeResolver}. A {@link DurableJob} is a recurring job
 * if it is registered with a {@link ExecutionTimeResolver}.
 *
 * <p>All implementations of {@link ExecutionTimeResolver} MUST use the parameter {@link Clock} for
 * resolving execution times to ensure the local time zone is accounted for.
 */
public final class ExecutionTimeResolvers {

  /** Every Sunday at 2am local time. Used for the OLD_JOB_CLEANUP job. */
  public static final class Sunday2Am extends ExecutionTimeResolver {

    public Sunday2Am() {
      super(Optional.empty());
    }

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
  public static final class FirstOfMonth2Am extends ExecutionTimeResolver {

    public FirstOfMonth2Am() {
      super(Optional.empty());
    }

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
  public static final class SecondOfMonth2Am extends ExecutionTimeResolver {

    public SecondOfMonth2Am() {
      super(Optional.empty());
    }

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
  public static final class ThirdOfMonth2Am extends ExecutionTimeResolver {

    public ThirdOfMonth2Am() {
      super(Optional.empty());
    }

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

  /** Nightly at 3am local time. Used for the MIGRATE_PRIMARY_APPLICANT_INFO job. */
  public static final class Nightly3Am extends ExecutionTimeResolver {

    public Nightly3Am() {
      super(Optional.empty());
    }

    @Override
    public Instant resolveExecutionTime(Clock clock) {
      return LocalDate.now(clock)
          .plusDays(1)
          .atStartOfDay(clock.getZone())
          .plus(3, ChronoUnit.HOURS)
          .toInstant();
    }
  }

  /** April 1st 2024. Used for the FIX_APPLICANT_DOB_DATA_PATH job. */
  public static final class AprilFirst extends ExecutionTimeResolver {

    public AprilFirst() {
      super(Optional.of(LocalDate.of(2024, 04, 01)));
    }

    @Override
    public Instant resolveExecutionTime(Clock clock) {
      return date.get().atStartOfDay(clock.getZone()).toInstant();
    }
  }
}
