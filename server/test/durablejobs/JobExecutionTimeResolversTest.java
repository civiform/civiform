package durablejobs;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.Test;

public class JobExecutionTimeResolversTest {

  @Test
  public void everySundayAt2Am() {
    // Wednesday Dec 7 at 10:15am
    Clock clock = Clock.fixed(Instant.parse("2022-12-07T10:15:30.00Z"), ZoneId.of("UTC"));
    // Sunday Dec 11 at 2:00am
    Instant expected = Instant.parse("2022-12-11T02:00:00.00Z");

    Instant result = new RecurringJobExecutionTimeResolvers.Sunday2Am().resolveExecutionTime(clock);

    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void firstOfMonth2Am() {
    // Wednesday Dec 7 at 10:15am
    Clock clock = Clock.fixed(Instant.parse("2022-12-07T10:15:30.00Z"), ZoneId.of("UTC"));
    // Jan 1 at 2:00am
    Instant expected = Instant.parse("2023-01-01T02:00:00.00Z");

    Instant result =
        new RecurringJobExecutionTimeResolvers.FirstOfMonth2Am().resolveExecutionTime(clock);

    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void secondOfMonth2Am() {
    // Wednesday Dec 7 at 10:15am
    Clock clock = Clock.fixed(Instant.parse("2022-12-07T10:15:30.00Z"), ZoneId.of("UTC"));
    // Jan 2 at 2:00am
    Instant expected = Instant.parse("2023-01-02T02:00:00.00Z");

    Instant result =
        new RecurringJobExecutionTimeResolvers.SecondOfMonth2Am().resolveExecutionTime(clock);

    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void thirdOfMonth2Am() {
    // Wednesday Dec 7 at 10:15am
    Clock clock = Clock.fixed(Instant.parse("2022-12-07T10:15:30.00Z"), ZoneId.of("UTC"));
    // Jan 3 at 2:00am
    Instant expected = Instant.parse("2023-01-03T02:00:00.00Z");

    Instant result =
        new RecurringJobExecutionTimeResolvers.ThirdOfMonth2Am().resolveExecutionTime(clock);

    assertThat(result).isEqualTo(expected);
  }
}
