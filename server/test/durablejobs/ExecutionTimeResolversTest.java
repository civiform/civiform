package durablejobs;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.Test;

public class ExecutionTimeResolversTest {

  @Test
  public void everySundayAt2Am() {
    // Wednesday Dec 7 at 10:15am
    Clock clock = Clock.fixed(Instant.parse("2022-12-07T10:15:30.00Z"), ZoneId.of("UTC"));
    // Sunday Dec 11 at 2:00am
    Instant expected = Instant.parse("2022-12-11T02:00:00.00Z");

    Instant result = new ExecutionTimeResolvers.Sunday2Am().resolveExecutionTime(clock);

    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void firstOfMonth2Am() {
    // Wednesday Dec 7 at 10:15am
    Clock clock = Clock.fixed(Instant.parse("2022-12-07T10:15:30.00Z"), ZoneId.of("UTC"));
    // Jan 1 at 2:00am
    Instant expected = Instant.parse("2023-01-01T02:00:00.00Z");

    Instant result = new ExecutionTimeResolvers.FirstOfMonth2Am().resolveExecutionTime(clock);

    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void secondOfMonth2Am() {
    // Wednesday Dec 7 at 10:15am
    Clock clock = Clock.fixed(Instant.parse("2022-12-07T10:15:30.00Z"), ZoneId.of("UTC"));
    // Jan 2 at 2:00am
    Instant expected = Instant.parse("2023-01-02T02:00:00.00Z");

    Instant result = new ExecutionTimeResolvers.SecondOfMonth2Am().resolveExecutionTime(clock);

    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void thirdOfMonth2Am() {
    // Wednesday Dec 7 at 10:15am
    Clock clock = Clock.fixed(Instant.parse("2022-12-07T10:15:30.00Z"), ZoneId.of("UTC"));
    // Jan 3 at 2:00am
    Instant expected = Instant.parse("2023-01-03T02:00:00.00Z");

    Instant result = new ExecutionTimeResolvers.ThirdOfMonth2Am().resolveExecutionTime(clock);

    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void nightly3Am() {
    // Wednesday Dec 7 at 10:15am
    Clock clock = Clock.fixed(Instant.parse("2022-12-07T10:15:30.00Z"), ZoneId.of("UTC"));
    // Thursday Dec 8 at 3:00am
    Instant expected = Instant.parse("2022-12-08T03:00:00.00Z");

    Instant result = new ExecutionTimeResolvers.Nightly3Am().resolveExecutionTime(clock);

    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void aprilFirst() {
    // Wednesday Dec 7 at 10:15am
    Clock clock = Clock.fixed(Instant.parse("2022-12-07T10:15:30.00Z"), ZoneId.of("UTC"));
    // April 1, 2024 at 12:00am
    Instant expected = Instant.parse("2024-04-01T00:00:00.00Z");

    Instant result = new ExecutionTimeResolvers.AprilFirst().resolveExecutionTime(clock);

    assertThat(result).isEqualTo(expected);
  }
}
