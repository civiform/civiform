package services;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.inject.Inject;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Utility class for converting dates between different formats. */
public class DateConverter {

  private final Clock clock;

  @Inject
  public DateConverter(Clock clock) {
    this.clock = checkNotNull(clock);
  }

  /**
   * Parses a string containing a ISO-8601 date (i.e. "YYYY-MM-DD") and converts it to an {@link
   * Instant} at the beginning of the day in local time zone.
   */
  public Instant parseIso8601DateToStartOfDateInstant(String dateString) {
    return LocalDate.parse(dateString, DateTimeFormatter.ISO_DATE)
        .atStartOfDay(clock.getZone())
        .toInstant();
  }
}
