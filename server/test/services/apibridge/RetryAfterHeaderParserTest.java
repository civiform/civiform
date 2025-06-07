package services.apibridge;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.Test;

public class RetryAfterHeaderParserTest {
  private final Clock clock = Clock.fixed(Instant.parse("2015-10-21T07:28:00Z"), ZoneId.of("UTC"));

  @Test
  public void defaults_to_zero_when_argument_null_or_blank() {
    var parser = new RetryAfterHeaderParser(clock);
    assertThat(parser.parse(null)).isEqualTo(Duration.ZERO);
    assertThat(parser.parse("")).isEqualTo(Duration.ZERO);
  }

  @Test
  public void parses_delay_seconds_format_successfully() {
    var parser = new RetryAfterHeaderParser(clock);
    Duration expected = Duration.ofSeconds(30);
    assertThat(parser.parse("30")).isEqualTo(expected);
    assertThat(parser.parse("   30")).isEqualTo(expected);
    assertThat(parser.parse("30   ")).isEqualTo(expected);
  }

  @Test
  public void defaults_to_zero_when_delay_seconds_format_is_invalid() {
    var parser = new RetryAfterHeaderParser(clock);
    assertThat(parser.parse("3 0")).isEqualTo(Duration.ZERO);
  }

  @Test
  public void parses_http_date_format_successfully() {
    var parser = new RetryAfterHeaderParser(clock);

    String retryAfterHeaderValue = "Wed, 21 Oct 2015 07:28:30 GMT";
    Duration expected = Duration.ofSeconds(30);
    assertThat(parser.parse(retryAfterHeaderValue)).isEqualTo(expected);
  }

  @Test
  public void defaults_to_zero_when_http_date_format_is_invalid() {
    var parser = new RetryAfterHeaderParser(clock);
    assertThat(parser.parse("Wed, 40 Oct 2015 07:28:30 GMT")).isEqualTo(Duration.ZERO);
  }

  @Test
  public void defaults_to_zero_when_http_date_is_in_the_past() {
    var parser = new RetryAfterHeaderParser(clock);
    assertThat(parser.parse("Wed, 14 Oct 2015 07:28:30 GMT")).isEqualTo(Duration.ZERO);
  }
}
