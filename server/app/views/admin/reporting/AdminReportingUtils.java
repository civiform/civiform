package views.admin.reporting;

import java.text.SimpleDateFormat;
import java.time.Duration;
import services.reporting.ApplicationSubmissionsStat;

/** Utility methods for admin reporting views. */
public final class AdminReportingUtils {
  /** Duration shown when {@code reporting_use_deterministic_stats} is enabled. */
  public static final String DETERMINISTIC_DURATION = "11:11:11";

  /** Month shown when {@code reporting_use_deterministic_stats} is enabled. */
  public static final String DETERMINISTIC_MONTH = "MM/YY";

  /**
   * Formats a duration for display. Browser snapshot tests enable {@code
   * reporting_use_deterministic_stats} because submission times vary between runs, so a fixed value
   * is returned in that case.
   */
  public static String formatDuration(double durationSeconds, boolean useDeterministicStats) {
    if (useDeterministicStats) {
      return DETERMINISTIC_DURATION;
    }

    return formatDuration(durationSeconds);
  }

  /**
   * Formats the month of a stat as MM/yyyy. Returns a fixed value when {@code
   * reporting_use_deterministic_stats} is enabled.
   */
  public static String formatMonth(ApplicationSubmissionsStat stat, boolean useDeterministicStats) {
    if (useDeterministicStats) {
      return DETERMINISTIC_MONTH;
    }

    return new SimpleDateFormat("MM/yyyy").format(stat.timestamp().get());
  }

  /**
   * Formats a duration in seconds to a human-readable string format.
   *
   * @param durationSeconds the duration in seconds
   * @return formatted string in the format [days:]HH:MM:SS
   */
  public static String formatDuration(double durationSeconds) {
    Duration duration = Duration.ofSeconds((long) durationSeconds);

    long days = duration.toDaysPart();
    long hours = duration.toHoursPart();
    int minutes = duration.toMinutesPart();
    int seconds = duration.toSecondsPart();

    StringBuilder result = new StringBuilder();

    if (days > 0) {
      result.append(days);
      result.append(":");
    }

    result.append(String.format("%02d:%02d:%02d", hours, minutes, seconds));

    return result.toString();
  }
}
