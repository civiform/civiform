package views.admin.reporting;

import com.google.common.collect.ImmutableList;
import lombok.Builder;
import lombok.Data;
import services.reporting.ApplicationSubmissionsStat;
import views.admin.BaseViewModel;

@Data
@Builder
public class AdminReportingProgramPageViewModel implements BaseViewModel {
  private final ImmutableList<ApplicationSubmissionsStat> monthlySubmissionsForProgram;
  private final String programSlug;
  private final String programName;
  private final String enUSLocalizedProgramName;

  public String formatDuration(double durationSeconds) {
    return AdminReportingUtils.formatDuration(durationSeconds);
  }

  public String getDownloadProgramCsvUrl(String programSlug) {
    return controllers.admin.routes.AdminReportingController.downloadProgramCsv(programSlug).url();
  }
}
