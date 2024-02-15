package services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import org.junit.Test;

public class DateConverterTest {

  private Clock clock = Clock.fixed(Instant.parse("2022-12-07T10:15:30.00Z"), ZoneId.of("UTC"));
  private DateConverter dateConverter = new DateConverter(clock);

  @Test
  public void parseIso8601DateToLocalDateInstant_isSuccessful() {
    String original = "2021-01-01Z";
    Instant parsed = dateConverter.parseIso8601DateToStartOfLocalDateInstant(original);
    String formatted = dateConverter.formatIso8601Date(parsed);

    assertThat(formatted).isEqualTo(original);
  }

  @Test
  public void parseIso8601DateToLocalDate_DateTimeParseExpectionIsGenerated() {
    String inputDate = "2012-20-20";
    assertThatThrownBy(() -> dateConverter.parseIso8601DateToLocalDate(inputDate))
        .isInstanceOf(DateTimeParseException.class);
  }

  @Test
  public void parseIso8601DateToLocalDate_isSuccessful() {
    String inputDate = "2022-01-01";
    LocalDate result = dateConverter.parseIso8601DateToLocalDate(inputDate);
    assertThat(inputDate).isEqualTo(result.toString());
  }

  @Test
  public void renderDate_isSuccessful() {
    String expectedResult = "2020-01-01";
    LocalDate date = LocalDate.of(2020, 1, 1);
    String result = dateConverter.formatIso8601Date(date);
    assertThat(expectedResult).isEqualTo(result);
  }

  @Test
  public void renderAsTwoDigitMonthAndYear_isSuccessful() throws ParseException {
    String expectedResult = "01/2022";
    String result =
        dateConverter.renderAsTwoDigitMonthAndYear(
            java.sql.Timestamp.valueOf("2022-01-01 10:10:10.0"));
    assertThat(expectedResult).isEqualTo(result);
  }

  @Test
  public void renderDateTime_isCorrect() {
    Instant instant = Instant.parse("2022-04-09T03:07:05.00Z");
    assertThat(dateConverter.renderDateTime(instant)).isEqualTo("2022/04/09 at 03:07 AM UTC");
  }

  @Test
  public void renderDateTimeDataOnly_isCorrect() {
    Instant instant = Instant.parse("2022-04-09T03:07:03.00Z");
    assertThat(dateConverter.renderDateTimeDataOnly(instant))
        .isEqualTo("2022/04/09 03:07:03 AM UTC");
  }

  @Test
  public void renderLocalDate_isCorrect() {
    Long timestamp = 1673453292339L;
    assertThat(dateConverter.renderLocalDate(timestamp)).isEqualTo(LocalDate.of(2023, 1, 11));
  }

  @Test
  public void getDateTimestampFromAge_isCorrect() {
    assertThat(dateConverter.renderLocalDate(dateConverter.getDateTimestampFromAge(30L)))
        .isEqualTo(LocalDate.now(clock).minusYears(30L));
  }
}
