package views.admin.reporting;

import java.time.Duration;

/** Utility methods for admin reporting views. */
public final class AdminReportingUtils {
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
