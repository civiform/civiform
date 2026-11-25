package views.admin.reporting;

import lombok.Builder;
import lombok.Data;
import modules.MainModule;
import services.reporting.ApplicationSubmissionsStat;
import services.reporting.ReportingService.MonthlyStats;
import views.BaseViewModel;

@Data
@Builder
public class AdminReportingPageViewModel implements BaseViewModel {
  private final MonthlyStats monthlyStats;
  private final boolean useDeterministicStats;

  public String getDetailsUrl(String programName) {
    return controllers.admin.routes.AdminReportingController.show(
            MainModule.SLUGIFIER.slugify(programName))
        .url();
  }

  public String formatDuration(double durationSeconds) {
    return AdminReportingUtils.formatDuration(durationSeconds, useDeterministicStats);
  }

  public String formatMonth(ApplicationSubmissionsStat stat) {
    return AdminReportingUtils.formatMonth(stat, useDeterministicStats);
  }

  public String getDownloadCsvUrl(String csvName) {
    return controllers.admin.routes.AdminReportingController.downloadCsv(csvName).url();
  }
}
