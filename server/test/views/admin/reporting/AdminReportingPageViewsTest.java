package views.admin.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static support.FakeRequestBuilder.fakeRequest;

import com.google.common.collect.ImmutableList;
import controllers.WithMockedProfiles;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import services.reporting.ApplicationSubmissionsStat;
import services.reporting.ReportingService.MonthlyStats;

public class AdminReportingPageViewsTest extends WithMockedProfiles {

  private static final ApplicationSubmissionsStat PROGRAM_STAT =
      ApplicationSubmissionsStat.create(
          "sample-program", "Sample Program", Optional.empty(), 3, 33, 33, 33, 33);

  private static final ApplicationSubmissionsStat MONTH_STAT =
      ApplicationSubmissionsStat.create(
          "All",
          "All",
          Optional.of(Timestamp.valueOf(LocalDateTime.of(2026, 11, 1, 0, 0))),
          3,
          17,
          17,
          17,
          17);

  @Before
  public void setUp() {
    createGlobalAdminWithMockedProfile();
  }

  @Test
  public void reportingPage_rendersTablesAndNotes() {
    AdminReportingPageViewModel model =
        AdminReportingPageViewModel.builder()
            .monthlyStats(
                MonthlyStats.create(
                    ImmutableList.of(),
                    ImmutableList.of(MONTH_STAT),
                    ImmutableList.of(PROGRAM_STAT)))
            .build();

    String result = instanceOf(AdminReportingPageView.class).render(fakeRequest(), model);

    assertThat(result).contains("Data may be up to an hour delayed.");
    assertThat(result).contains("Submissions by program (all programs)");
    assertThat(result).contains("Submissions by month (all programs)");
    assertThat(result).contains("Sample Program");
    assertThat(result).contains("11/2026");
    assertThat(result).contains("00:00:33");
    assertThat(result).contains("href=\"#by-program-note-1\"");
    assertThat(result).contains("id=\"by-program-note-4\"");
    assertThat(result).contains("href=\"#by-month-note-1\"");
    assertThat(result).contains("id=\"by-month-note-4\"");
    assertThat(result)
        .contains("The fastest 25 percent of applicants finished in this time or less.");
    assertThat(result).doesNotContain("usa-prose");
  }

  @Test
  public void programReportingPage_rendersHeadingTableAndNotes() {
    AdminReportingProgramPageViewModel model =
        AdminReportingProgramPageViewModel.builder()
            .monthlySubmissionsForProgram(ImmutableList.of(MONTH_STAT))
            .programSlug("sample-program")
            .programName("sample-program")
            .enUSLocalizedProgramName("Sample Program")
            .build();

    String result = instanceOf(AdminReportingProgramPageView.class).render(fakeRequest(), model);

    assertThat(result).contains("Sample Program reporting");
    assertThat(result).contains("Data may be up to an hour delayed.");
    assertThat(result).contains("Submissions by month");
    assertThat(result).contains("11/2026");
    assertThat(result).contains("00:00:17");
    assertThat(result).contains("href=\"#by-month-note-1\"");
    assertThat(result).contains("id=\"by-month-note-4\"");
    assertThat(result)
        .contains("Almost all applicants (99 percent) finished in this time or less.");
    assertThat(result).doesNotContain("usa-prose");
  }
}
