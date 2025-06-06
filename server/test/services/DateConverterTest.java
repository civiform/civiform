package services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import org.junit.Test;

public class DateConverterTest {

  private Clock clockUTC = Clock.fixed(Instant.parse("2022-12-07T10:15:30.00Z"), ZoneId.of("UTC"));
  private DateConverter dateConverterUTC = new DateConverter(clockUTC);

  private Clock clockPT =
      Clock.fixed(Instant.parse("2022-12-07T10:15:30.00Z"), ZoneId.of("America/Los_Angeles"));
  private DateConverter dateConverterPT = new DateConverter(clockPT);

  @Test
  public void parseIso8601DateToStartOfLocalDateInstant_isSuccessful() {
    String original = "2021-01-01Z";
    Instant parsed = dateConverterUTC.parseIso8601DateToStartOfLocalDateInstant(original);
    String formatted = dateConverterUTC.formatIso8601Date(parsed);

    assertThat(formatted).isEqualTo(original);
  }

  @Test
  public void parseIso8601DateToStartOfLocalDateInstant_DateTimeParseExceptionIsGenerated() {
    String inputDate = "abcd";
    assertThatThrownBy(() -> dateConverterUTC.parseIso8601DateToStartOfLocalDateInstant(inputDate))
        .isInstanceOf(DateTimeParseException.class);
  }

  @Test
  public void parseIso8601DateToEndOfLocalDateInstant_isSuccessful() {
    // Construct the expected Instant
    LocalDate targetDate = LocalDate.of(2024, 5, 20);
    ZonedDateTime zonedDateTime = targetDate.atStartOfDay(ZoneId.of("UTC"));
    zonedDateTime = zonedDateTime.plusDays(1).minusNanos(1);
    Instant expected = zonedDateTime.toInstant();

    String inputDate = "2024-05-20";
    Instant actual = dateConverterUTC.parseIso8601DateToEndOfLocalDateInstant(inputDate);

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void parseIso8601DateToEndOfLocalDateInstant_DateTimeParseExceptionIsGenerated() {
    String inputDate = "abcd";
    assertThatThrownBy(() -> dateConverterUTC.parseIso8601DateToEndOfLocalDateInstant(inputDate))
        .isInstanceOf(DateTimeParseException.class);
  }

  @Test
  public void parseIso8601DateToLocalDateTimeInstant_dateOnly_isSuccessful() {
    String dateOnly = "2025-05-26";
    Instant result = dateConverterUTC.parseIso8601DateToLocalDateTimeInstant(dateOnly);

    // When no time is provided, it should be set to start of day
    Instant expected = LocalDate.of(2025, 5, 26).atStartOfDay(ZoneId.of("UTC")).toInstant();

    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void parseIso8601DateToLocalDateTimeInstant_dateTime_isSuccessful() {
    String dateTime = "2025-05-26T11:00:00";
    Instant result = dateConverterUTC.parseIso8601DateToLocalDateTimeInstant(dateTime);

    Instant expected = LocalDateTime.of(2025, 5, 26, 11, 0, 0).atZone(ZoneId.of("UTC")).toInstant();
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void parseIso8601DateToLocalDateTimeInstant_dateTimeTimezone_isSuccessful() {
    // Date uses UTC timezone
    String dateTime = "2025-05-26T02:00:00.00Z";

    // When the string uses the same local timezone than the local one, the parsed time will use
    // such timezone.
    Instant resultUTC = dateConverterUTC.parseIso8601DateToLocalDateTimeInstant(dateTime);
    Instant expectedUTC =
        LocalDateTime.of(2025, 5, 26, 2, 0, 0).atZone(ZoneId.of("UTC")).toInstant();
    assertThat(resultUTC).isEqualTo(expectedUTC);

    // When the string uses a different timezone than the local one, the parsed time should use the
    // timezone given.
    Instant resultPT = dateConverterPT.parseIso8601DateToLocalDateTimeInstant(dateTime);
    Instant expectedPT =
        LocalDateTime.of(2025, 5, 26, 2, 0, 0).atZone(ZoneId.of("UTC")).toInstant();
    assertThat(resultPT).isEqualTo(expectedPT);
  }

  @Test
  public void parseIso8601DateToLocalDateTimeInstant_DateTimeParseExceptionIsGenerated() {
    String inputDate = "abcd";
    assertThatThrownBy(() -> dateConverterUTC.parseIso8601DateToLocalDateTimeInstant(inputDate))
        .isInstanceOf(DateTimeParseException.class);
  }

  @Test
  public void parseIso8601DateToLocalDate_DateTimeParseExpectionIsGenerated() {
    String inputDate = "2012-20-20";
    assertThatThrownBy(() -> dateConverterUTC.parseIso8601DateToLocalDate(inputDate))
        .isInstanceOf(DateTimeParseException.class);
  }

  @Test
  public void parseIso8601DateToLocalDate_isSuccessful() {
    String inputDate = "2022-01-01";
    LocalDate result = dateConverterUTC.parseIso8601DateToLocalDate(inputDate);
    assertThat(inputDate).isEqualTo(result.toString());
  }

  @Test
  public void renderDate_isSuccessful() {
    String expectedResult = "2020-01-01";
    LocalDate date = LocalDate.of(2020, 1, 1);
    String result = dateConverterUTC.formatIso8601Date(date);
    assertThat(expectedResult).isEqualTo(result);
  }

  @Test
  public void renderShortDateInLocalTime_handlesTimezoneAndLocale() {
    Instant instant = Instant.parse("2022-10-15T03:07:05.00Z");
    Locale localeUS = Locale.US;
    Locale localeUK = Locale.UK;

    assertThat(dateConverterUTC.renderShortDateInLocalTime(instant, localeUS))
        .isEqualTo("10/15/22");
    assertThat(dateConverterPT.renderShortDateInLocalTime(instant, localeUS)).isEqualTo("10/14/22");

    assertThat(dateConverterUTC.renderShortDateInLocalTime(instant, localeUK))
        .isEqualTo("15/10/2022");
    assertThat(dateConverterPT.renderShortDateInLocalTime(instant, localeUK))
        .isEqualTo("14/10/2022");
  }

  @Test
  public void renderShortDateInLocalTime_dropsZeroDigitsInMonthAndDay() {
    Instant instant = Instant.parse("2009-04-06T03:07:05.00Z");
    Locale localeUS = Locale.US;

    assertThat(dateConverterUTC.renderShortDateInLocalTime(instant, localeUS)).isEqualTo("4/6/09");
    assertThat(dateConverterPT.renderShortDateInLocalTime(instant, localeUS)).isEqualTo("4/5/09");
  }

  @Test
  public void renderAsTwoDigitMonthAndYear_isSuccessful() throws ParseException {
    String expectedResult = "01/2022";
    String result =
        dateConverterUTC.renderAsTwoDigitMonthAndYear(
            java.sql.Timestamp.valueOf("2022-01-01 10:10:10.0"));
    assertThat(expectedResult).isEqualTo(result);
  }

  @Test
  public void renderDateTimeHumanReadable_isCorrect() {
    Instant instant = Instant.parse("2022-04-09T03:07:05.00Z");
    assertThat(dateConverterUTC.renderDateTimeHumanReadable(instant))
        .isEqualTo("2022/04/09 at 3:07 AM UTC");
    assertThat(dateConverterPT.renderDateTimeHumanReadable(instant))
        .isEqualTo("2022/04/08 at 8:07 PM PDT");

    instant = Instant.parse("2022-12-09T03:07:05.00Z");
    assertThat(dateConverterUTC.renderDateTimeHumanReadable(instant))
        .isEqualTo("2022/12/09 at 3:07 AM UTC");
    assertThat(dateConverterPT.renderDateTimeHumanReadable(instant))
        .isEqualTo("2022/12/08 at 7:07 PM PST");
  }

  @Test
  public void renderDateTimeDataOnly_isCorrect() {
    Instant instant = Instant.parse("2022-04-09T03:07:03.00Z");
    assertThat(dateConverterUTC.renderDateTimeDataOnly(instant))
        .isEqualTo("2022/04/09 03:07:03 AM UTC");
    assertThat(dateConverterPT.renderDateTimeDataOnly(instant))
        .isEqualTo("2022/04/08 08:07:03 PM PDT");

    instant = ZonedDateTime.parse("2022-04-09T03:07:03.00-08:00").toInstant();
    assertThat(dateConverterUTC.renderDateTimeDataOnly(instant))
        .isEqualTo("2022/04/09 11:07:03 AM UTC");
    assertThat(dateConverterPT.renderDateTimeDataOnly(instant))
        .isEqualTo("2022/04/09 04:07:03 AM PDT");
  }

  @Test
  public void renderDateTimeIso8601ExtendedOffset_isCorrect() {
    Instant instant = Instant.parse("2022-04-09T03:07:03.00Z");
    assertThat(dateConverterUTC.renderDateTimeIso8601ExtendedOffset(instant))
        .isEqualTo("2022-04-09T03:07:03Z");
    assertThat(dateConverterPT.renderDateTimeIso8601ExtendedOffset(instant))
        .isEqualTo("2022-04-08T20:07:03-07:00");
  }

  @Test
  public void renderLocalDate_isCorrect() {
    Long timestamp = 1673453292339L;
    assertThat(dateConverterUTC.renderLocalDate(timestamp)).isEqualTo(LocalDate.of(2023, 1, 11));
  }

  @Test
  public void getDateTimestampFromAge_isCorrect() {
    assertThat(dateConverterUTC.renderLocalDate(dateConverterUTC.getDateTimestampFromAge(30L)))
        .isEqualTo(LocalDate.now(clockUTC).minusYears(30L));
  }
}
