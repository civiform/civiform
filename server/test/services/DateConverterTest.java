package services;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.ZoneId;
import org.junit.Test;

public class DateConverterTest {

  private DateConverter dateConverter = new DateConverter(ZoneId.of("UTC"));

  @Test
  public void workingWith_Iso8601_dates() {
    String original = "2021-01-01Z";
    Instant parsed = dateConverter.parseIso8601DateToStartOfDateInstant(original);
    String formatted = dateConverter.formatIso8601Date(parsed);

    assertThat(formatted).isEqualTo(original);
  }
}
