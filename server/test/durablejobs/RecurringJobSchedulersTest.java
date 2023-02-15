package durablejobs;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.Test;

public class RecurringJobSchedulersTest {

  @Test
  public void everySundayAt2Am() {
    // Wednesday Nov 7 at 10:15am
    Clock clock = Clock.fixed(Instant.parse("2022-12-07T10:15:30.00Z"), ZoneId.of("UTC"));
    // Sunday Nov11 at 2:00am
    Instant expected = Instant.parse("2022-12-11T02:00:00.00Z");

    Instant result = new RecurringJobSchedulers.Sunday2Am().resolveExecutionTime(clock);

    assertThat(result).isEqualTo(expected);
  }
}
