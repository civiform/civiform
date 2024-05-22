package services;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.inject.Inject;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/** Utility class for converting dates between different formats. */
public final class DateConverter {

  private final Clock clock;
  private final ZoneId zoneId;
  private static final DateTimeFormatter DATE_TIME_FORMATTER_WITH_SLASH =
      DateTimeFormatter.ofPattern("yyyy/MM/dd");
  private static final DateTimeFormatter DATE_TIME_FORMATTER_WITH_DASH =
      DateTimeFormatter.ofPattern("yyyy-MM-dd");

  @Inject
  public DateConverter(Clock clock) {
    this.clock = checkNotNull(clock);
    this.zoneId = checkNotNull(clock.getZone());
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

  /**
   * Parses string day, month and year and converts them to a {@link LocalDate}
   *
   * @throws DateTimeParseException if conjoined date string is not well-formed.
   */
  public LocalDate parseDayMonthYearToLocalDate(String day, String month, String year) {
    day = day.length() == 1 ? "0" + day : day; // The day needs to be 2 digits
    return parseIso8601DateToLocalDate(year + "-" + month + "-" + day);
  }

  /** Returns the current LocalDate based on the specified time-zone(zoneId) */
  public LocalDate getCurrentDateForZoneId() {
    return LocalDate.now(this.zoneId);
  }

  /**
   * Parses a string containing a ISO-8601 date (i.e. "YYYY-MM-DD") and converts it to an {@link
   * Instant} at the beginning of the day in Local time zone.
   *
   * @throws DateTimeParseException if dateString is not well-formed.
   */
  public Instant parseIso8601DateToStartOfLocalDateInstant(String dateString) {
    return parseIso8601DateToLocalDate(dateString).atStartOfDay(zoneId).toInstant();
  }

  /**
   * Parses a string containing a ISO-8601 date (i.e. "YYYY-MM-DD") and converts it to an {@link
   * Instant} at the end of the day in Local time zone.
   *
   * @throws DateTimeParseException if dateString is not well-formed.
   */
  public Instant parseIso8601DateToEndOfLocalDateInstant(String dateString) {
    return parseIso8601DateToLocalDate(dateString).atTime(LocalTime.MAX).atZone(zoneId).toInstant();
  }

  /** Formats an {@link Instant} to a human-readable date and time in the local time zone. */
  public String renderDateTimeHumanReadable(Instant time) {
    ZonedDateTime dateTime = time.atZone(zoneId);
    return dateTime.format(DateTimeFormatter.ofPattern("yyyy/MM/dd 'at' h:mm a z"));
  }

  /**
   * Formats an {@link Instant} to a date and time in the local time zone for the purpose of data
   * exports (with no filler words).
   */
  public String renderDateTimeDataOnly(Instant time) {
    ZonedDateTime dateTime = time.atZone(zoneId);
    return dateTime.format(DateTimeFormatter.ofPattern("yyyy/MM/dd hh:mm:ss a z"));
  }

  /**
   * Formats an {@link Instant} to a date and time in the local time zone in the ISO 8601 format for
   * the purpose of API responses. Examples: 2011-12-03T10:15:30+01:00 2011-12-03T09:15:30Z
   */
  public String renderDateTimeIso8601ExtendedOffset(Instant time) {
    ZonedDateTime dateTime = time.atZone(zoneId);
    return dateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
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

  /** Formats a {@link java.sql.Timestamp} to MM/YYYY. */
  public String renderAsTwoDigitMonthAndYear(Timestamp timestamp) {
    return new SimpleDateFormat("MM/yyyy").format(timestamp);
  }

  /** Gets the {@link Long} timestamp from an age, by subtracting the age from today's date. */
  public long getDateTimestampFromAge(Long age) {
    return LocalDate.now(clock).minusYears(age).atStartOfDay(zoneId).toInstant().toEpochMilli();
  }

  /**
   * Gets the {@link Long} timestamp from an age, by subtracting the age from today's date, when the
   * age may not be a whole number.
   */
  public long getDateTimestampFromAge(Double age) {
    Double fullYear = Math.floor(age);
    LocalDate dateFromAge = LocalDate.now(clock).minusYears(fullYear.longValue());
    if ((age - fullYear) > 0) {
      dateFromAge = dateFromAge.minusMonths((long) Math.floor((age - fullYear) * 12));
    }
    return dateFromAge.atStartOfDay(zoneId).toInstant().toEpochMilli();
  }
}
