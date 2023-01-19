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
public final class DateConverter {

  private final ZoneId zoneId;
  private static final DateTimeFormatter DATE_TIME_FORMATTER_WITH_SLASH =
      DateTimeFormatter.ofPattern("yyyy/MM/dd");
  private static final DateTimeFormatter DATE_TIME_FORMATTER_WITH_DASH =
      DateTimeFormatter.ofPattern("yyyy-MM-dd");

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

  /**
   * Parses a string containing a ISO-8601 date (i.e. "YYYY-MM-DD") and converts it to an {@link
   * LocalDate}
   *
   * @throws DateTimeParseException if dateString is not well-formed.
   */
  public LocalDate parseIso8601DateToLocalDate(String dateString) {
    return LocalDate.parse(dateString, DateTimeFormatter.ISO_DATE);
  }

  /** Returns the current LocalDate based on the specified time-zone(zoneId) */
  public LocalDate getCurrentDateForZoneId() {
    return LocalDate.now(this.zoneId);
  }

  /**
   * Parses a string containing a ISO-8601 date (i.e. "YYYY-MM-DD") and converts it to an {@link
   * Instant} at the beginning of the day in local time zone.
   *
   * @throws DateTimeParseException if dateString is not well-formed.
   */
  public Instant parseIso8601DateToStartOfDateInstant(String dateString) {
    return parseIso8601DateToLocalDate(dateString).atStartOfDay(zoneId).toInstant();
  }

  /** Formats an {@link Instant} to a human-readable date and time in the local time zone. */
  public String renderDateTime(Instant time) {
    ZonedDateTime dateTime = time.atZone(zoneId);
    return dateTime.format(DateTimeFormatter.ofPattern("yyyy/MM/dd 'at' h:mm a z"));
  }

  /**
   * Formats an {@link Instant} to a date and time in the local time zone for the purpose of data
   * exports (with no filler words).
   */
  public String renderDateTimeDataOnly(Instant time) {
    ZonedDateTime dateTime = time.atZone(zoneId);
    return dateTime.format(DateTimeFormatter.ofPattern("yyyy/MM/dd h:mm:ss a z"));
  }

  /** Formats an {@link Instant} to a date in the local time zone. */
  public String renderDate(Instant time) {
    ZonedDateTime dateTime = time.atZone(zoneId);
    return dateTime.format(DATE_TIME_FORMATTER_WITH_SLASH);
  }
  /** Formats an {@link LocalDate} to a String. */
  public String formatIso8601Date(LocalDate date) {
    return date.format(DATE_TIME_FORMATTER_WITH_DASH);
  }

  /** Formats a {@link Long} timestamp to a {@link LocalDate}. */
  public LocalDate renderLocalDate(Long timestamp) {
    return Instant.ofEpochMilli(timestamp).atZone(this.zoneId).toLocalDate();
  }
}
