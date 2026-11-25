package views.admin.reporting;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.Test;
import services.reporting.ApplicationSubmissionsStat;

public class AdminReportingUtilsTest {

  private static final ApplicationSubmissionsStat STAT =
      ApplicationSubmissionsStat.create(
          "program",
          "Program",
          Optional.of(Timestamp.valueOf(LocalDateTime.of(2024, 3, 15, 12, 0))),
          10,
          1,
          2,
          3,
          4);

  @Test
  public void formatDuration_formatsHoursMinutesSeconds() {
    assertThat(AdminReportingUtils.formatDuration(3723, false)).isEqualTo("01:02:03");
  }

  @Test
  public void formatDuration_includesDaysWhenPresent() {
    assertThat(AdminReportingUtils.formatDuration(90061, false)).isEqualTo("1:01:01:01");
  }

  @Test
  public void formatDuration_deterministic_returnsFixedValue() {
    assertThat(AdminReportingUtils.formatDuration(3723, true))
        .isEqualTo(AdminReportingUtils.DETERMINISTIC_DURATION);
  }

  @Test
  public void formatMonth_formatsTwoDigitMonthAndYear() {
    assertThat(AdminReportingUtils.formatMonth(STAT, false)).isEqualTo("03/24");
  }

  @Test
  public void formatMonth_deterministic_returnsFixedValue() {
    assertThat(AdminReportingUtils.formatMonth(STAT, true))
        .isEqualTo(AdminReportingUtils.DETERMINISTIC_MONTH);
  }
}
