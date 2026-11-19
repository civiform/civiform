package views.admin.reporting;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import lombok.Builder;
import lombok.Data;
import modules.MainModule;
import services.reporting.ReportingService.MonthlyStats;
import views.BaseViewModel;

@Data
@Builder
public class AdminReportingPageViewModel implements BaseViewModel {
  private final MonthlyStats monthlyStats;

  /** Stubs durations and months so browser test screenshots are deterministic. */
  private final boolean useDeterministicStats;

  public String getDetailsUrl(String programName) {
    return controllers.admin.routes.AdminReportingController.show(
            MainModule.SLUGIFIER.slugify(programName))
        .url();
  }

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

  public String getDownloadCsvUrl(String csvName) {
    return controllers.admin.routes.AdminReportingController.downloadCsv(csvName).url();
  }
}
