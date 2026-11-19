package views.admin.reporting;

import com.google.common.collect.ImmutableList;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import lombok.Builder;
import lombok.Data;
import services.reporting.ApplicationSubmissionsStat;
import views.BaseViewModel;

@Data
@Builder
public class AdminReportingProgramPageViewModel implements BaseViewModel {
  private final ImmutableList<ApplicationSubmissionsStat> monthlySubmissionsForProgram;
  private final String programSlug;
  private final String programName;
  private final String enUSLocalizedProgramName;

  /** Stubs durations and months so browser test screenshots are deterministic. */
  private final boolean useDeterministicStats;

  public String formatDuration(double durationSeconds) {
    if (useDeterministicStats) {
      return "11:11:11";
    }

    return AdminReportingUtils.formatDuration(durationSeconds);
  }

  public String formatMonth(Timestamp timestamp) {
    if (useDeterministicStats) {
      return "MM/YY";
    }

    return new SimpleDateFormat("MM/yyyy").format(timestamp);
  }

  public String getDownloadProgramCsvUrl(String programSlug) {
    return controllers.admin.routes.AdminReportingController.downloadProgramCsv(programSlug).url();
  }
}
