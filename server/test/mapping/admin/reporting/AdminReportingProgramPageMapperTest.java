package mapping.admin.reporting;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import services.reporting.ApplicationSubmissionsStat;
import views.admin.reporting.AdminReportingProgramPageViewModel;

public final class AdminReportingProgramPageMapperTest {

  private AdminReportingProgramPageMapper mapper;

  @Before
  public void setup() {
    mapper = new AdminReportingProgramPageMapper();
  }

  @Test
  public void map_setsAllFields() {
    ImmutableList<ApplicationSubmissionsStat> stats = ImmutableList.of();

    AdminReportingProgramPageViewModel result =
        mapper.map(stats, "test-slug", "Test Program", "Test Program EN");

    assertThat(result.getMonthlySubmissionsForProgram()).isEqualTo(stats);
    assertThat(result.getProgramSlug()).isEqualTo("test-slug");
    assertThat(result.getProgramName()).isEqualTo("Test Program");
    assertThat(result.getEnUSLocalizedProgramName()).isEqualTo("Test Program EN");
  }
}
