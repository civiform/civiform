package mapping.admin.reporting;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import services.reporting.ReportingService.MonthlyStats;
import views.admin.reporting.AdminReportingPageViewModel;

public final class AdminReportingPageMapperTest {

  private AdminReportingPageMapper mapper;

  @Before
  public void setup() {
    mapper = new AdminReportingPageMapper();
  }

  @Test
  public void map_setsMonthlyStats() {
    MonthlyStats stats =
        MonthlyStats.create(ImmutableList.of(), ImmutableList.of(), ImmutableList.of());

    AdminReportingPageViewModel result = mapper.map(stats);

    assertThat(result.getMonthlyStats()).isEqualTo(stats);
  }
}
