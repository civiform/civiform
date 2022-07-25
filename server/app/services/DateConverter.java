package services;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.inject.Inject;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/** Utility class for converting dates between different formats. */
public class DateConverter {

  private final ZoneId zoneId;

  @Inject
  public DateConverter(ZoneId zoneId) {
    this.zoneId = checkNotNull(zoneId);
  }

  /**
   * Format an instant to an RFC 1123 string in app's time zone. E.g. 'Tue, 3 Jun 2008 11:05:30 GMT'
   */
  public String formatRfc1123(Instant instant) {
    return DateTimeFormatter.RFC_1123_DATE_TIME.withZone(zoneId).format(instant);
  }

  /** Formats an instant to an ISO-8601 date of the form "YYYY-MM-DDZ" where the 'Z' is literal. */
  public String formatIso8601Date(Instant instant) {
    return DateTimeFormatter.ISO_DATE.withZone(zoneId).format(instant);
  }

  public LocalDate parseStringtoLocalDate(String dateString) {
    return LocalDate.parse(dateString, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
  }

  /**
   * Parses a string containing a ISO-8601 date (i.e. "YYYY-MM-DD") and converts it to an {@link
   * Instant} at the beginning of the day in local time zone.
   *
   * @throws DateTimeParseException if dateString is not well-formed.
   */
  public Instant parseIso8601DateToStartOfDateInstant(String dateString) {
    return LocalDate.parse(dateString, DateTimeFormatter.ISO_DATE).atStartOfDay(zoneId).toInstant();
  }

  /** Formats an {@link Instant} to a date and time in the local time zone. */
  public String renderDateTime(Instant time) {
    ZonedDateTime dateTime = time.atZone(zoneId);
    return dateTime.format(DateTimeFormatter.ofPattern("yyyy/MM/dd 'at' h:mm a z"));
  }

  /** Formats an {@link Instant} to a date in the local time zone. */
  public String renderDate(Instant time) {
    ZonedDateTime dateTime = time.atZone(zoneId);
    return dateTime.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
  }

  public String formatDate(LocalDate date) {
    return date.format(DateTimeFormatter.ofPattern("MM-dd-yyyy"));
  }
}
