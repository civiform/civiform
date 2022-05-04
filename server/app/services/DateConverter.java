package services;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/** Utility class for converting dates between different formats. */
public class DateConverter {

  /**
   * Parses a string containing a ISO-8601 date (i.e. "YYYY-MM-DD") and converts it to an {@link
   * Instant} at the beginning of the day specified by the date in UTC.
   */
  public static Instant parseIso8601DateToStartOfDateInstant(String dateString) {
    return LocalDate.parse(dateString, DateTimeFormatter.ISO_DATE)
        .atStartOfDay()
        .toInstant(ZoneOffset.UTC);
  }
}
