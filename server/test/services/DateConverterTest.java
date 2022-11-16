package services;

import static org.assertj.core.api.Assertions.*;
import static org.junit.Assert.assertThrows;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import org.junit.Test;

public class DateConverterTest {

  private final DateConverter dateConverter = new DateConverter(ZoneId.of("UTC"));

  @Test
  public void workingWith_Iso8601_dates() {
    String original = "2021-01-01Z";
    Instant parsed = dateConverter.parseIso8601DateToStartOfDateInstant(original);
    String formatted = dateConverter.formatIso8601Date(parsed);

    assertThat(formatted).isEqualTo(original);
  }

  @Test
  public void parseIso8601DateToLocalDate_DateTimeParseExpectionIsGenerated() {
    String inputDate = "2012-20-20";
    assertThrows(
        DateTimeParseException.class, () -> dateConverter.parseIso8601DateToLocalDate(inputDate));
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
  public void renderDateTime_isCorrect() {
    Instant instant = Instant.parse("2022-04-09T10:15:30.00Z");
    assertThat(dateConverter.renderDateTime(instant)).isEqualTo("2022/04/09 at 10:15 AM UTC");
  }

  @Test
  public void renderDateTimeDataOnly_isCorrect() {
    Instant instant = Instant.parse("2022-04-09T10:15:30.00Z");
    assertThat(dateConverter.renderDateTimeDataOnly(instant))
        .isEqualTo("2022/04/09 10:15:30 AM UTC");
  }
}
